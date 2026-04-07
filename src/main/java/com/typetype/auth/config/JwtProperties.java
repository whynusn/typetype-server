package com.typetype.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置类
 *
 * 🎓 学习点：
 * - @ConfigurationProperties：将 application.yml 中的配置绑定到字段
 * - prefix = "jwt"：绑定前缀为 jwt 的配置节点
 *
 * 💡 使用方式：
 * - application.yml 中定义：
 *   jwt:
 *     secret-key: your-secret-key
 *     access-token-expire: 900
 * - Spring Boot 自动注入到这个类的字段
 *
 * 💡 为什么用 @Component？
 * - 使其成为 Spring Bean，可以被注入到其他类
 *
 * 💡 依赖注入方式（本项目使用）：
 * - 使用 @RequiredArgsConstructor + final 字段
 * - Spring 通过构造器自动注入
 *
 * 示例：
 * @Service
 * @RequiredArgsConstructor
 * public class JwtService {
 *     private final JwtProperties jwtProperties; // 构造器注入
 * }
 *
 * 💡 配置文件位置：
 * - application.yml（通用配置）
 * - application-dev.yml（开发环境）
 * - application-prod.yml（生产环境）
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT 签名密钥
     *
     * 🎓 密钥要求：
     * - 长度至少 256 位（32 字节）
     * - 生产环境应该使用强随机字符串
     * - 不要在代码中硬编码，从环境变量或配置中心读取
     */
    private String secretKey;

    /**
     * Access Token 过期时间（秒）
     * 默认：15 分钟
     */
    private Long accessTokenExpire = 900L;

    /**
     * Refresh Token 过期时间（秒）
     * 默认：7 天
     */
    private Long refreshTokenExpire = 604800L;

    /**
     * Token 签发者
     * 用于标识 Token 的来源
     */
    private String issuer = "typetype-server";

    /**
     * HTTP Header 名称
     * 默认：Authorization
     */
    private String headerName = "Authorization";

    /**
     * Token 前缀
     * 默认：Bearer
     */
    private String tokenPrefix = "Bearer ";
}
