package com.typetype.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typetype.common.result.Result;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 IP 的请求限流过滤器
 *
 * 使用 Bucket4j 令牌桶算法，对认证相关接口（登录、注册）进行 IP 级别限流，
 * 防止暴力破解和批量注册。
 *
 * 限流规则：
 * - /api/v1/auth/login：每 IP 每分钟最多 10 次
 * - /api/v1/auth/register：每 IP 每分钟最多 5 次
 * - 其他 /api/v1/auth/**：每 IP 每分钟最多 20 次
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        if (path.equals("/api/v1/auth/login")) {
            if (!tryConsume(loginBuckets, clientIp, 10, 1)) {
                sendRateLimitResponse(response);
                return;
            }
        } else if (path.equals("/api/v1/auth/register")) {
            if (!tryConsume(registerBuckets, clientIp, 5, 1)) {
                sendRateLimitResponse(response);
                return;
            }
        } else if (path.startsWith("/api/v1/auth/")) {
            if (!tryConsume(authBuckets, clientIp, 20, 1)) {
                sendRateLimitResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 尝试消费令牌，返回 true 表示允许请求
     */
    private boolean tryConsume(Map<String, Bucket> buckets, String key,
                                int capacity, int tokens) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(capacity));
        return bucket.tryConsume(tokens);
    }

    private Bucket createBucket(int capacity) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * 获取客户端真实 IP（考虑反向代理）
     */
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
