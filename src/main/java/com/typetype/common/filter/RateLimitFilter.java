package com.typetype.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typetype.common.result.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * 基于 IP 的请求限流过滤器（Redis 版本）。
 *
 * 🎓 面试知识点：分布式限流
 *
 * 1. 单机限流（ConcurrentHashMap）：
 *    - 每个实例独立计数
 *    - 多实例部署时，总请求数 = 实例数 × 单实例限制
 *    - 问题：无法精确控制全局速率
 *
 * 2. 分布式限流（Redis INCR + EXPIRE）：
 *    - 所有实例共享同一个计数器
 *    - Redis 是单线程的，INCR 是原子操作
 *    - 保证全局速率精确
 *
 * 限流规则：
 * - /api/v1/auth/login：每 IP 每分钟最多 10 次
 * - /api/v1/auth/register：每 IP 每分钟最多 5 次
 * - 其他 /api/v1/auth/**：每 IP 每分钟最多 20 次
 *
 * 💡 面试考点：
 * Q: Redis INCR + EXPIRE 有什么问题？
 * A: 存在竞态条件 — INCR 后、EXPIRE 前如果应用崩溃，key 永不过期。
 *    解决方案：使用 Lua 脚本保证原子性，或使用 Redis 2.6+ 的 EXPIRE 选项。
 *    对于限流场景，偶尔的 key 泄漏影响不大，可以接受。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_PREFIX = "ratelimit:";
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 端点 → 每分钟最大请求数 */
    private static final Map<String, Integer> RATE_LIMITS = Map.of(
        "/api/v1/auth/login", 10,
        "/api/v1/auth/register", 5
    );
    private static final int DEFAULT_AUTH_LIMIT = 20;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/api/v1/auth/")) {
            String clientIp = getClientIp(request);
            int limit = getLimit(path);

            if (!tryAcquire(clientIp, limit)) {
                sendRateLimitResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 尝试获取令牌（Redis INCR + EXPIRE）。
     *
     * 🎓 工作原理：
     * 1. INCR key → 计数器 +1，返回当前值
     * 2. 如果返回值 == 1（首次请求），设置 EXPIRE 1分钟
     * 3. 如果返回值 > limit，拒绝请求
     */
    private boolean tryAcquire(String clientIp, int limit) {
        try {
            String key = RATE_LIMIT_PREFIX + clientIp;
            Long count = redisTemplate.opsForValue().increment(key);

            if (count != null && count == 1) {
                // 首次请求，设置过期时间
                redisTemplate.expire(key, WINDOW);
            }

            return count == null || count <= limit;
        } catch (Exception e) {
            // Redis 不可用时降级：允许请求通过
            log.warn("Redis 限流降级: {}", e.getMessage());
            return true;
        }
    }

    private int getLimit(String path) {
        return RATE_LIMITS.getOrDefault(path, DEFAULT_AUTH_LIMIT);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Result<Void> result = Result.error(4298, "请求过于频繁，请稍后再试");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
