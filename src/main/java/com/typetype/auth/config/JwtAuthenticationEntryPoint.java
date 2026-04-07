package com.typetype.auth.config;

import com.typetype.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT 认证入口点
 *
 * 🎓 学习点：
 * - AuthenticationEntryPoint：Spring Security 的认证失败处理接口
 * - 认证失败时被自动调用
 * - 返回自定义的 401 响应（非默认的 HTML）
 *
 * 💡 调用时机：
 * - 请求未携带 Token 时
 * - Token 无效或过期时（JwtFilter 清空 SecurityContext）
 * - .authenticated() 规则拦截时
 *
 * 💡 与 GlobalExceptionHandler 的区别：
 * - GlobalExceptionHandler：捕获 Controller 层异常（@RestControllerAdvice）
 * - AuthenticationEntryPoint：捕获 Security 层认证失败（Filter 层）
 * - AccessDeniedHandler：捕获权限不足异常
 *
 * 💡 面试考点：
 * Q: 为什么 AuthenticationEntryPoint 不能像 Controller 那样返回对象自动序列化？
 * A: 因为它在 Filter 层执行，不在 Spring MVC 流程内，DispatcherServlet 不会处理
 * Q: 认证入口点何时被调用？
 * A: SecurityFilterChain 检测到 SecurityContext 没有认证信息时
 * Q: 为什么不用 @RestControllerAdvice 统一处理？
 * A: Filter 异常无法被 ControllerAdvice 捕获，需要专门的入口点
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 认证失败入口方法
     *
     * 🎓 执行流程：
     * 1. JwtAuthenticationFilter 验证失败，清空 SecurityContext
     * 2. SecurityFilterChain 检测到没有认证信息
     * 3. 调用这个 commence() 方法
     * 4. 返回 401 响应给客户端
     *
     * @param request       HTTP 请求
     * @param response      HTTP 响应
     * @param authException 认证异常（包含失败原因）
     * @throws IOException  IO 异常
     */
    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {

        // 记录认证失败日志
        log.warn("认证失败: 请求路径={}, 原因={}",
            request.getRequestURI(),
            authException.getMessage());

        // 设置 HTTP 状态码：401 Unauthorized
        // 🎓 401 vs 403：
        // - 401: 未认证（没有有效的身份凭证）
        // - 403: 禁止访问（已认证但没有权限）
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 设置响应类型为 JSON
        response.setContentType("application/json;charset=UTF-8");

        // 构建统一的响应格式
        // 🎓 为什么手动拼接 JSON？
        // - 此时不经过 Spring MVC，没有自动序列化机制
        // - 不能像 Controller 那样返回对象
        String jsonResponse = String.format(
            "{\"code\":%d,\"message\":\"%s\",\"timestamp\":%d}",
            ResultCode.TOKEN_INVALID.getCode(),
            "认证失败，请重新登录",
            System.currentTimeMillis()
        );

        // 写入响应
        response.getWriter().write(jsonResponse);
    }
}
