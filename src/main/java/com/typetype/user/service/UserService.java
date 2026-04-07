package com.typetype.user.service;

import com.typetype.common.exception.BusinessException;
import com.typetype.common.result.ResultCode;
import com.typetype.user.constant.UserRole;
import com.typetype.user.dto.UserVO;
import com.typetype.user.converter.UserConverter;
import com.typetype.user.entity.User;
import com.typetype.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务
 *
 * 🎓 学习点：
 * - @Service：标记为 Spring Service 组件
 * - @RequiredArgsConstructor：Lombok 自动生成包含 final 字段的构造器
 * - BCryptPasswordEncoder：Spring Security 提供的密码加密工具
 *
 * 💡 职责划分：
 * - UserService：负责用户数据管理（CRUD、验证等）
 * - AuthService：负责认证逻辑（登录、注册、Token 生成等）
 *
 * 💡 依赖注入方式对比：
 * - 构造器注入（✅ 推荐）：final 字段 + @RequiredArgsConstructor
 * - @Autowired 字段注入（❌ 不推荐）：无法 final，测试不便
 * - @Autowired setter 注入（❌ 不推荐）：可选依赖时才用
 *
 * 💡 构造器注入为什么推荐？
 * 1. 字段可以是 final，保证不可变
 * 2. 依赖明确，易于测试（可以手动传 mock 对象）
 * 3. Spring 4.3+ 单个构造器时可以省略 @Autowired
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserConverter userConverter;

    /**
     * 创建用户（供 AuthService 调用）
     *
     * 🎓 设计决策：
     * - 这个方法是数据层面的创建
     * - 业务校验和 Token 生成由 AuthService 处理
     *
     * @param username 用户名
     * @param password 加密后的密码
     * @param nickname 昵称（可选）
     * @return 创建的用户实体
     */
    public User createUser(String username, String password, String nickname) {
        User user = User.builder()
            .username(username)
            .password(password)
            .nickname(nickname)
            .role(UserRole.USER.name())
            .build();

        userMapper.insert(user);

        // 返回包含数据库生成字段的用户
        return userMapper.findById(user.getId());
    }

    /**
     * 获取用户实体（内部使用）
     *
     * @param id 用户ID
     * @return 用户实体
     */
    public User getUserEntityById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    /**
     * 验证用户凭证
     *
     * 🎓 职责：
     * - 仅验证用户名密码是否正确
     * - 不负责 Token 生成（由 AuthService 处理）
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 验证成功返回用户实体
     * @throws BusinessException 用户不存在或密码错误
     */
    public User validateCredentials(String username, String password) {
        // 1. 根据用户名查询用户
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 2. 校验密码（BCrypt.matches：每次加密结果不同，但可以验证）
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        return user;
    }

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 用户名是否已存在
     */
    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username);
    }

    /**
     * 根据 ID 获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息 VO
     */
    public UserVO getUserById(Long id) {
        return userConverter.toVO(getUserEntityById(id));
    }

    /**
     * 更新用户权限
     */
    public boolean updateUserRole(Long id, String role) {
        return userMapper.updateRole(id, role);
    }
}
