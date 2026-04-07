package com.typetype.auth.controller;

import com.typetype.auth.dto.LoginDTO;
import com.typetype.auth.dto.RegisterDTO;
import com.typetype.auth.dto.TokenVO;
import com.typetype.auth.service.AuthService;
import com.typetype.common.result.Result;
import com.typetype.user.dto.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * 🎓 学习点：
 * - @RestController：组合 @Controller + @ResponseBody，返回 JSON
 * - @RequestMapping：类级别 URL 前缀
 * - @PostMapping/@GetMapping：方法级别 URL 映射
 *
 * 💡 参数绑定注解：
 * - @RequestBody：请求体（POST/PUT），JSON → 对象
 * - @PathVariable：路径参数（/users/{id}）
 * - @RequestParam：查询参数（?page=1）
 * - @Valid：触发参数校验（配合 DTO 中的 @NotBlank 等）
 *
 * 💡 分层架构原则：
 * - Controller 层只负责 HTTP 请求/响应处理
 * - 业务逻辑委托给 Service 层
 * - 便于测试和代码复用
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     *
     * @param registerDTO 注册请求（@Valid 触发校验）
     * @return 用户信息
     *
     * 🎓 HTTP 规范：
     * - POST 请求体放在 @RequestBody 中
     * - @Valid 触发 RegisterDTO 中的校验注解
     * - 校验失败由 GlobalExceptionHandler 捕获
     */
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        UserVO userVO = authService.register(registerDTO);
        return Result.success("注册成功", userVO);
    }

    /**
     * 用户登录
     *
     * @param loginDTO 登录请求
     * @return Token 信息（accessToken、refreshToken、用户信息）
     */
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        TokenVO tokenVO = authService.login(loginDTO);
        return Result.success("登录成功", tokenVO);
    }

    /**
     * 刷新 Token
     *
     * 🎓 设计要点：
     * - Refresh Token 通常放在 Header 中（更安全）
     * - 验证 Refresh Token 有效性
     * - 生成新的 Access Token 和 Refresh Token（Token Rotation）
     * - 业务逻辑由 AuthService 处理
     *
     * @param authHeader Authorization Header，包含 Refresh Token
     * @return 新的 Token 信息
     */
    @PostMapping("/refresh")
    public Result<TokenVO> refreshToken(@RequestHeader("Authorization") String authHeader) {
        TokenVO tokenVO = authService.refreshToken(authHeader);
        return Result.success("Token 刷新成功", tokenVO);
    }

    /**
     * 登出
     *
     * 🎓 登出处理：
     * - 客户端删除本地 Token
     * - 服务端撤销 Refresh Token（由 AuthService 处理）
     *
     * @param authHeader Authorization Header（可选）
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return Result.success("登出成功");
    }
}
