package com.typetype.common.filter;

import com.typetype.common.util.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.UUID;

/**
 * MDC 过滤器：为每个请求生成 traceId，写入 MDC 供日志使用
 *
 * 🎓 面试考点：
 * - MDC（Mapped Diagnostic Context）：线程级别的日志上下文
 * - OncePerRequestFilter：确保每个请求只执行一次
 * - @Order(Ordered.HIGHEST_PRECEDENCE)：最先执行，保证后续日志都有 traceId
 *
 * 💡 分布式追踪原理：
 * 1. 请求进入 → 生成 traceId → 写入 MDC
 * 2. 日志输出 → 自动携带 traceId（logback-spring.xml 配置 %X{traceId}）
 * 3. 请求离开 → 清理 MDC（防止线程池复用时污染）
 *
 * 💡 为什么不用 ThreadLocal？
 * - MDC 底层就是 ThreadLocal，但提供了统一的 API
 * - logback 直接支持 %X{traceId} 格式
 * - Spring Cloud Sleuth 等框架也基于 MDC
 *
 * 💡 traceId 生成策略：
 * - UUID.randomUUID()：简单但较长（36字符）
 * - 雪花算法：趋势递增，适合分布式系统
 * - 实际生产中常用 Zipkin/Jaeger 的 traceId（16字节 hex）
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String USER_ID = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, java.io.IOException {
        try {
            // 生成 traceId（请求唯一标识）
            String traceId = UUID.randomUUID().toString().replace("-", "");
            MDC.put(TRACE_ID, traceId);

            // 尝试获取当前用户 ID（未登录时为 null）
            try {
                Long userId = SecurityUtils.getCurrentUserId();
                if (userId != null) {
                    MDC.put(USER_ID, userId.toString());
                }
            } catch (Exception e) {
                // 未登录或 token 无效，忽略
            }

            // 设置响应头，便于前端/调试工具追踪
            response.setHeader("X-Trace-Id", traceId);

            filterChain.doFilter(request, response);
        } finally {
            // 清理 MDC，防止线程池复用时污染
            MDC.clear();
        }
    }
}
