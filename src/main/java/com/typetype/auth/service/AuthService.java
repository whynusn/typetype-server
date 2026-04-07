package com.typetype.auth.service;

import com.typetype.auth.config.JwtProperties;
import com.typetype.auth.dto.JwtPayloadDTO;
import com.typetype.auth.dto.LoginDTO;
import com.typetype.auth.dto.RegisterDTO;
import com.typetype.auth.dto.TokenVO;
import com.typetype.common.exception.BusinessException;
import com.typetype.common.result.ResultCode;
import com.typetype.user.converter.UserConverter;
import com.typetype.user.dto.UserVO;
import com.typetype.user.entity.User;
import com.typetype.user.service.UserService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 认证服务
 *
 * 🎓 学习点：
 * - 封装认证相关的业务逻辑
 * - 将 Controller 层的业务逻辑下沉到 Service 层
 * - 便于单元测试和代码复用
 *
 * 💡 职与其他模块的职责划分：
 * - AuthController：仅负责 HTTP 请求/响应处理
 * - AuthService：负责认证业务逻辑（登录、注册、Token 验证、生成、撤销等）
 * - JwtService：负责 JWT 底层操作（生成、解析、验证）
 * - UserService：负责用户数据 CRUD、用户名检查、密码验证等
 *
 * 💡 为什么要分层？
 * 1. 单一职责：每层只关注自己的职责
 * 2. 易于测试：Service 层可以独立测试，不依赖 Web 层
 * 3. 代码复用：Service 层逻辑可以被多个 Controller 或 Service 调用
 * 4. 扩展性强：添加 Redis 黑名单、OAuth 等逻辑只需修改 AuthService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserConverter userConverter;

    /**
     * 用户注册
     *
     * 🎓 注册流程：
     * 1. 校验两次密码是否一致
     * 2. 检查用户名是否已存在
     * 3. 密码加密
     * 4. 创建用户
     * 5. 转换为 VO 返回
     *
     * @param registerDTO 注册请求
     * @return 用户信息 VO
     */
    public UserVO register(RegisterDTO registerDTO) {
        // 1. 校验两次密码是否一致
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "两次密码不一致");
        }

        // 2. 检查用户名是否已存在
        if (userService.existsByUsername(registerDTO.getUsername())) {
            throw new BusinessException(ResultCode.USER_EXISTS);
        }

        // 3. 密码加密（BCrypt 单向哈希）
        String encodedPassword = passwordEncoder.encode(registerDTO.getPassword());

        // 4. 创建用户
        User user = userService.createUser(
            registerDTO.getUsername(),
            encodedPassword,
            registerDTO.getNickname()
        );

        log.info("用户 {} 注册成功", user.getUsername());

        // 5. 转换为 VO 返回
        return userConverter.toVO(user);
    }

    /**
     * 用户登录
     *
     * 🎓 登录流程：
     * 1. 验证用户名密码
     * 2. 生成 Access Token
     * 3. 生成 Refresh Token
     * 4. （生产环境）保存 Refresh Token 到 Redis
     * 5. 构建返回结果
     *
     * @param loginDTO 登录请求
     * @return Token 信息（包含 accessToken、refreshToken、用户信息）
     */
    public TokenVO login(LoginDTO loginDTO) {
        // 1. 验证用户凭证
        User user = userService.validateCredentials(
            loginDTO.getUsername(),
            loginDTO.getPassword()
        );

        // 2. 生成 Access Token
        String accessToken = jwtService.generateAccessToken(
            user.getId(),
            user.getUsername(),
            user.getRole()
        );

        // 3. 生成 Refresh Token
        String refreshToken = jwtService.generateRefreshToken(
            user.getId(),
            user.getUsername(),
            user.getRole()
        );

        // 4. （生产环境）保存 Refresh Token 到 Redis
        // String userId = user.getId().toString();
        // redisTemplate.opsForValue().set("refresh_token:" + userId, refreshToken,
        //     Duration.ofSeconds(jwtProperties.getRefreshTokenExpire()));

        log.info("用户 {} 登录成功", user.getUsername());

        // 5. 构建返回结果
        UserVO userVO = userConverter.toVO(user);

        return TokenVO.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(jwtProperties.getAccessTokenExpire())
            .user(userVO)
            .build();
    }

    /**
     * 刷新 Token
     *
     * 🎓 设计要点：
     * 1. 提取 Refresh Token
     * 2. 验证 Token 有效性
     * 3. 检查 Token 类型（必须是 refresh）
     * 4. 生成新的 Access Token
     * 5. 生成新的 Refresh Token（Token Rotation）
     * 6. （生产环境）更新 Redis，撤销旧的 Refresh Token
     *
     * 💡 Token Rotation（令牌轮换）：
     * - 刷新时生成新的 Refresh Token
     * - 旧的 Refresh Token 立即失效
     * - 防止 Token 重放攻击
     *
     * @param authHeader Authorization Header，包含 Refresh Token
     * @return 新的 Token 信息
     */
    public TokenVO refreshToken(String authHeader) {
        // 1. 提取 Refresh Token
        String token = extractToken(authHeader);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID,
                "Refresh Token 不能为空");
        }

        // 2. 验证 Token
        JwtPayloadDTO payload;
        try {
            payload = jwtService.verifyToken(token);
        } catch (JwtException e) {
            log.warn("Refresh Token 验证失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.TOKEN_INVALID,
                "Refresh Token 无效或已过期");
        }

        // 3. 检查是否为 Refresh Token
        if (!"refresh".equals(payload.getTokenType())) {
            throw new BusinessException(ResultCode.TOKEN_INVALID,
                "Token 类型错误，需要 Refresh Token");
        }

        // 4. （生产环境）检查 Redis 中是否存在该 Refresh Token
        // if (!redisTemplate.hasKey("refresh_token:" + payload.getUserId())) {
        //     throw new BusinessException(ResultCode.TOKEN_INVALID,
        //         "Refresh Token 已失效");
        // }

        // 5. 生成新的 Access Token
        User user = userService.getUserEntityById(payload.getUserId());
        String newAccessToken = jwtService.generateAccessToken(
            user.getId(),
            user.getUsername(),
            user.getRole()
        );

        // 6. 生成新的 Refresh Token（Token Rotation）
        String newRefreshToken = jwtService.generateRefreshToken(
            user.getId(),
            user.getUsername(),
            user.getRole()
        );

        // 7. （生产环境）更新 Redis：删除旧的，保存新的
        // String userId = payload.getUserId().toString();
        // redisTemplate.opsForValue().set("refresh_token:" + userId, newRefreshToken,
        //     Duration.ofSeconds(jwtProperties.getRefreshTokenExpire()));

        // 8. 查询用户信息（可选，如果需要在返回结果中包含用户信息）
        UserVO userVO = userConverter.toVO(user);

        log.info("用户 {} 刷新 Token 成功", payload.getUsername());

        // 9. 返回新的 Token 对
        return TokenVO.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .expiresIn(jwtProperties.getAccessTokenExpire())
            .user(userVO)
            .build();
    }

    /**
     * 登出
     *
     * 🎓 设计要点：
     * 1. 提取 Token（可选，客户端可能已经删除了本地 Token）
     * 2. 验证 Token
     * 3. （生产环境）从 Redis 删除 Refresh Token
     * 4. （可选）将 Access Token 加入黑名单
     *
     * 💡 JWT 登出的局限性：
     * - JWT 是无状态的，无法在服务端主动失效
     * - 需要 Redis 黑名单机制或短有效期
     * - 最佳实践：只删除 Refresh Token，Access Token 短期有效即可
     *
     * @param authHeader Authorization Header（可选）
     */
    public void logout(String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            // Token 为空也允许登出（客户端已删除本地 Token）
            log.info("登出请求：Token 为空");
            return;
        }

        String token = extractToken(authHeader);

        try {
            JwtPayloadDTO payload = jwtService.verifyToken(token);

            // （生产环境）从 Redis 删除 Refresh Token
            // String userId = payload.getUserId().toString();
            // redisTemplate.delete("refresh_token:" + userId);

            // （可选）将 Access Token 加入黑名单
            // String cacheKey = "blacklist:token:" + token;
            // Long remainingTime = getRemainingTime(token);
            // if (remainingTime > 0) {
            //     redisTemplate.opsForValue().set(cacheKey, "1",
            //         Duration.ofSeconds(remainingTime));
            // }

            log.info("用户 {} 登出成功", payload.getUsername());

        } catch (JwtException e) {
            // Token 无效也允许登出（客户端可能已经删除了本地 Token）
            log.warn("登出时 Token 验证失败，但允许登出: {}", e.getMessage());
        }
    }

    /**
     * 从 Authorization Header 提取 Token
     *
     * 🎓 格式：
     * - 标准：Bearer <token>
     * - 也可以直接传 <token>（兼容处理）
     *
     * @param authHeader Authorization Header 值
     * @return Token 字符串
     */
    private String extractToken(String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            return null;
        }

        String tokenPrefix = jwtProperties.getTokenPrefix();
        if (authHeader.startsWith(tokenPrefix)) {
            return authHeader.substring(tokenPrefix.length());
        }

        // 兼容直接传 token 的情况
        return authHeader;
    }

    /**
     * （预留方法）获取 Token 剩余有效时间（秒）
     *
     * 🎓 用途：
     * - 将 Access Token 加入黑名单时，设置合理的过期时间
     * - 避免黑名单无限增长
     *
     * @param token JWT 字符串
     * @return 剩余秒数
     */
    private long getRemainingTime(String token) {
        // TODO: 实现 Token 剩余时间计算
        // 可以从 JWT Payload 的 exp 字段计算
        return 900; // 默认 15 分钟
    }
}
