package com.typetype.auth.service;

import com.typetype.auth.config.JwtProperties;
import com.typetype.auth.dto.JwtPayloadDTO;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * JWT 服务
 *
 * 🎓 学习点：
 * - JWT 生成：Jwts.builder()
 * - JWT 解析：Jwts.parser()
 * - 密钥处理：Keys.hmacShaKeyFor()
 *
 * 💡 依赖注入：
 * - 使用 @RequiredArgsConstructor + final 字段
 * - Spring 通过构造器自动注入 JwtProperties
 *
 * 💡 为什么 @PostConstruct？
 * - 在 Bean 初始化后执行
 * - 将字符串密钥转换为 SecretKey 对象
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    /**
     * JWT 签名密钥（从字符串转换而来）
     */
    private SecretKey secretKey;

    /**
     * JWT Parser（线程安全，初始化一次复用）
     */
    private JwtParser jwtParser;

    /**
     * 初始化密钥
     *
     * 🎓 执行时机：
     * - JwtService Bean 创建完成后自动执行
     * - 确保 secretKey 在使用前已初始化
     */
    @PostConstruct
    public void init() {
        String secretKeyString = jwtProperties.getSecretKey();

        // 检查密钥长度（至少 256 位 = 32 字节）
        if (secretKeyString.length() < 32) {
            log.warn("JWT 密钥长度不足 32 字节，建议使用更长的密钥");
        }

        // 将字符串转换为 SecretKey
        this.secretKey = Keys.hmacShaKeyFor(
            secretKeyString.getBytes(StandardCharsets.UTF_8)
        );

        // 初始化 JwtParser 一次，后续复用
        this.jwtParser = Jwts.parser()
            .verifyWith(secretKey)
            .build();

        log.info("JWT 密钥初始化完成");
    }

    /**
     * 生成 Access Token
     *
     * 🎓 Token 包含的内容：
     * - userId: 用户 ID
     * - username: 用户名
     * - tokenType: 标识为 Access Token
     * - iat: 签发时间（由 jjwt 自动添加）
     * - exp: 过期时间（由 jjwt 自动添加）
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param role 用户角色
     * @return JWT 字符串
     */
    public String generateAccessToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, "access",
            jwtProperties.getAccessTokenExpire());
    }

    /**
     * 生成 Refresh Token
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param role 用户角色
     * @return JWT 字符串
     */
    public String generateRefreshToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, "refresh",
            jwtProperties.getRefreshTokenExpire());
    }

    /**
     * 生成 Token（通用方法）
     *
     * 🎓 JWT 生成流程：
     * 1. 创建 Builder
     * 2. 添加自定义 claims
     * 3. 设置签发时间
     * 4. 设置过期时间
     * 5. 设置签发者（可选）
     * 6. 使用密钥签名
     * 7. compact() 生成 JWT 字符串
     *
     * @param userId    用户 ID
     * @param username  用户名
     * @param role 用户角色
     * @param tokenType Token 类型
     * @param expireSec 过期时间（秒）
     * @return JWT 字符串
     */
    private String generateToken(Long userId, String username, String role,
                               String tokenType, long expireSec) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(expireSec);

        return Jwts.builder()
            // 添加自定义 claims
            .claim("userId", userId)
            .claim("username", username)
            .claim("tokenType", tokenType)
            .claim("role", normalizeRole(role))
            // 设置 subject（可选，通常放用户 ID 或用户名）
            .subject(userId.toString())
            // 设置签发时间
            .issuedAt(Date.from(now))
            // 设置过期时间
            .expiration(Date.from(expiration))
            // 设置签发者（可选）
            .issuer(jwtProperties.getIssuer())
            // 使用密钥签名
            .signWith(secretKey)
            // 生成 JWT 字符串
            .compact();
    }

    /**
     * 验证 Token 并解析 Payload
     *
     * 🎓 验证流程：
     * 1. 创建 Parser
     * 2. 设置验证密钥
     * 3. 解析 Token
     * 4. 提取所有 Claims（包括标准 claims 和自定义 claims）
     * 5. 构建 JwtPayloadDTO
     *
     * @param token JWT 字符串
     * @return Payload 对象（包含标准 claims 和自定义 claims）
     * @throws JwtException Token 无效或过期时抛出异常
     */
    public JwtPayloadDTO verifyToken(String token) {
        try {
            // 解析 Token 并获取 Payload（使用预初始化的parser）
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();

            // 提取标准 JWT Claims
            Date iat = claims.getIssuedAt();
            Date exp = claims.getExpiration();

            // 构建 JwtPayloadDTO
            // 🎓 包含标准 Claims 的原因：
            // 1. 审计需求：记录 Token 签发和过期时间
            // 2. 业务需求：前端提前刷新 Token（exp - now < 5min）
            // 3. 扩展性：避免未来需要时重新解析（性能浪费）
            return JwtPayloadDTO.builder()
                // ========== 自定义 Claims ==========
                .userId(claims.get("userId", Long.class))
                .username(claims.get("username", String.class))
                .tokenType(claims.get("tokenType", String.class))
                .role(normalizeRole(claims.get("role", String.class)))
                // ========== 标准 Claims (RFC 7519) ==========
                .iat(iat != null ? iat.getTime() : null)
                .exp(exp != null ? exp.getTime() : null)
                .iss(claims.getIssuer())
                .sub(claims.getSubject())
                .build();

        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.warn("Token 无效: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 从 Token 中提取用户 ID
     *
     * 🎓 为什么提供这个方法？
     * - 简化调用，只获取用户 ID
     * - 不需要调用方处理完整的 Payload
     *
     * @param token JWT 字符串
     * @return 用户 ID
     * @throws JwtException Token 无效或过期时抛出异常
     */
    public Long getUserIdFromToken(String token) {
        return verifyToken(token).getUserId();
    }

    /**
     * 检查 Token 是否为 Access Token
     *
     * @param token JWT 字符串
     * @return 是否为 Access Token
     */
    public boolean isAccessToken(String token) {
        try {
            return "access".equals(verifyToken(token).getTokenType());
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 检查 Token 是否为 Refresh Token
     *
     * @param token JWT 字符串
     * @return 是否为 Refresh Token
     */
    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(verifyToken(token).getTokenType());
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 规范化角色
     *
     * - 去空
     * - 存储 USER/ADMIN 这种无前缀角色
     */
    private String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        String trimmed = role.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

}
