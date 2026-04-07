package com.typetype.auth.filter;

import com.typetype.auth.config.JwtProperties;
import com.typetype.auth.dto.JwtPayloadDTO;
import com.typetype.auth.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * JWT 认证过滤器
 *
 * 🎓 学习点：
 * - OncePerRequestFilter：确保每个请求只执行一次
 * - SecurityContextHolder：存储当前用户认证信息
 * - UsernamePasswordAuthenticationToken：认证令牌
 *
 * 💡 执行流程：
 * 1. 每个 HTTP 请求都会经过这个过滤器
 * 2. 从 Header 提取 Token
 * 3. 验证 Token
 * 4. 将用户信息设置到 SecurityContext
 * 5. 继续过滤链
 *
 * 💡 依赖注入：
 * - 使用 @Component + @RequiredArgsConstructor
 * - Spring 自动注入 JwtService 和 JwtProperties
 *
 * 💡 白名单接口：
 * - 不需要认证的接口（如 /api/v1/auth/**）
 * - 在 SecurityConfig 中配置，不需要认证的请求会跳过这个过滤器的验证
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    /**
     * 过滤器核心方法
     *
     * 🎓 执行时机：
     * - 每个 HTTP 请求都会调用
     * - OncePerRequestFilter 保证只执行一次
     *
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain 过滤链
     */
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. 从 Header 提取 Token
        String authHeader = request.getHeader(jwtProperties.getHeaderName());
        String tokenPrefix = jwtProperties.getTokenPrefix();

        if (authHeader == null || !authHeader.startsWith(tokenPrefix)) {
            // 没有 Token 或格式不对，继续过滤链
            // 可能是白名单接口，后续会由 Security 检查
            log.debug("请求未携带有效的 Authorization Header");
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 提取 Token（去掉 "Bearer " 前缀）
        String token = authHeader.substring(tokenPrefix.length());

        try {
            // 3. 验证 Token 并解析 Payload
            JwtPayloadDTO payload = jwtService.verifyToken(token);

            // 检查是否为 Access Token
            if (!"access".equals(payload.getTokenType())) {
                log.warn("收到非 Access Token: {}", payload.getTokenType());
                filterChain.doFilter(request, response);
                return;
            }

            // 4. 创建认证令牌
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    payload.getUserId(),  // 主体（用户 ID）
                    null,                // 凭证（不需要保存密码）
                    buildAuthorities(payload) // 权限集合
                );

            // 5. 设置到 SecurityContext
            SecurityContextHolder.getContext()
                .setAuthentication(authentication);

            log.debug("用户 {} 认证成功", payload.getUsername());

        } catch (JwtException e) {
            // Token 无效或过期，清空 SecurityContext
            log.warn("Token 验证失败: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        // 6. 继续过滤链
        filterChain.doFilter(request, response);

        // 7. 清理 SecurityContext（避免内存泄漏）
        // 注意：doFilter 返回后清理，确保请求结束后清空
        SecurityContextHolder.clearContext();
    }

    /**
     * 决定是否过滤请求
     *
     * 🎓 作用：
     * - 可以跳过某些请求，不执行过滤逻辑
     * - 本例不过滤任何请求，全部执行
     *
     * @param request HTTP 请求
     * @return 是否执行过滤器
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 这里可以配置不需要执行这个过滤器的请求
        // 例如：静态资源、健康检查等
        // return request.getServletPath().startsWith("/static");
        return false;
    }

    /**
     * 构建权限集合
     *
     * 🎓 面试讲法：
     * - 授权信息应来自用户表或 JWT claim
     * - 角色统一用 USER/ADMIN，最终授权时加 ROLE_ 前缀
     */
    private List<GrantedAuthority> buildAuthorities(JwtPayloadDTO payload) {
        String role = payload.getRole();
        if (role == null || role.isBlank()) {
            return List.of();
        }
        String normalized = role.trim();
        String authority = normalized.startsWith("ROLE_")
            ? normalized
            : "ROLE_" + normalized;
        return List.of(new SimpleGrantedAuthority(authority));
    }
}
