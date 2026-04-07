package com.typetype.common.config;

import com.typetype.auth.config.JwtAuthenticationEntryPoint;
import com.typetype.auth.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类
 *
 * 🎓 学习点：
 *   - @Configuration：标识这是一个配置类
 *   - @EnableWebSecurity：启用 Spring Security Web 支持
 *   - SecurityFilterChain：配置安全过滤链
 *
 * 💡 配置说明：
 *   - 禁用 CSRF：JWT API 通常不需要 CSRF 防护
 *   - 添加 JWT 过滤器：在 UsernamePasswordAuthenticationFilter 之前
 *   - 配置认证入口点：认证失败时返回自定义响应
 *   - 白名单配置：登录/注册接口不需要认证
 *   - 其他接口：需要 JWT 认证
 *
 * 💡 认证流程失败处理（面试重点）：
 *   1. JwtAuthenticationFilter 验证失败 → 清空 SecurityContext
 *   2. SecurityFilterChain 检测到没有认证 → 调用 AuthenticationEntryPoint
 *   3. JwtAuthenticationEntryPoint.commence() → 返回 401 JSON
 *   4. 注意：GlobalExceptionHandler 捕获不到 Filter 层异常
 *
 * 💡 依赖注入：
 *   - 使用 @RequiredArgsConstructor + final 字段
 *   - 注入 JwtAuthenticationFilter 和 JwtAuthenticationEntryPoint
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * Security 过滤链配置
     *
     * 🎓 配置流程：
     * 1. 禁用 CSRF
     * 2. 添加 JWT 认证过滤器
     * 3. 配置异常处理（认证入口点）
     * 4. 配置请求授权：
     *    - 白名单：/api/v1/auth/** 不需要认证
     *    - 其他请求：需要认证
     *
     * @param http HttpSecurity 对象
     * @return SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（JWT API 通常不需要）
            // 🎓 为什么禁用？
            // - JWT 不依赖 Session Cookie
            // - CSRF 攻击需要 Cookie + 自动提交表单
            // - 禁用可以减少不必要的检查开销
            .csrf(csrf -> csrf.disable())

            // 添加 JWT 认证过滤器
            // 在 UsernamePasswordAuthenticationFilter 之前执行
            // 🎓 为什么在 UsernamePasswordAuthenticationFilter 之前？
            // - 我们使用 JWT 认证，不使用用户名密码表单登录
            // - 这个过滤器应该代替默认的表单认证过滤器
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            )

            // 配置异常处理
            // 🎓 认证入口点：
            // - 当 .authenticated() 拦截时调用
            // - 返回自定义的 401 响应而非默认 HTML
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )

            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 白名单：这些接口不需要认证
                // 🎓 白名单设计：
                // - /api/v1/auth/**：注册、登录、刷新、登出
                // - /api/v1/health：健康检查接口
                // - /error：默认错误页面
                .requestMatchers("/api/v1/auth/**", "/api/v1/health", "/error").permitAll()

                // 其他所有请求需要认证
                // 🎓 authenticated() 的作用：
                // - 检查 SecurityContextHolder.getContext().getAuthentication()
                // - 如果为 null，则调用 AuthenticationEntryPoint
                // - 如果有值，则放行请求
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * 密码加密器 Bean
     *
     * 🎓 BCryptPasswordEncoder：
     * - Spring Security 提供的密码加密工具
     * - 自动生成盐值，每次加密结果不同但可以验证
     * - strength=10 是推荐值，范围 4-31
     *
     * 💡 面试考点：
     * Q: BCrypt 和 MD5/SHA-256 的区别？
     * A: BCrypt 内置加盐 + 可调节强度，抗彩虹表攻击；MD5/SHA 不安全
     * Q: 为什么每次加密结果不同？
     * A: BCrypt 每次随机生成盐值，但验证时可以识别
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
