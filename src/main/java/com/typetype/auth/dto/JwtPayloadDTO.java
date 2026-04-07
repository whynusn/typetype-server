package com.typetype.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * JWT 载荷数据
 *
 * 🎓 学习点：
 * - DTO (Data Transfer Object)：数据传输对象
 * - 用于 JWT Payload 的数据封装
 *
 * 💡 为什么需要这个类？
 * - 封装从 JWT 解析出的数据
 * - 类型安全，避免手动类型转换
 * - 便于扩展（添加更多字段）
 *
 * 💡 JWT 标准 Claims (RFC 7519)：
 * - iss (Issuer)：签发者
 * - sub (Subject)：主题
 * - aud (Audience)：受众
 * - exp (Expiration)：过期时间
 * - iat (Issued At)：签发时间
 * - jti (JWT ID)：唯一标识
 *
 * 💡 自定义 Claims：
 * - userId：用户 ID
 * - username：用户名
 * - tokenType：Token 类型
 * - role：用户角色
 *
 * 💡 面试考点：
 * Q: JWT 的标准 claims 有哪些？
 * A: iss、sub、aud、exp、iat、jti
 * Q: 为什么在 DTO 中包含标准 claims？
 * A: 便于审计、Token 生命周期管理、前端提前刷新
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtPayloadDTO implements Serializable {

    // ==================== 自定义 Claims ====================

    /**
     * 用户 ID
     * 对应 JWT 中的自定义 claim: userId
     */
    private Long userId;

    /**
     * 用户名
     * 对应 JWT 中的自定义 claim: username
     */
    private String username;

    /**
     * Token 类型
     * 用于区分 Access Token 和 Refresh Token
     */
    private String tokenType;

    /**
     * 角色（如 USER / ADMIN）
     */
    private String role;

    // ==================== 标准 Claims (RFC 7519) ====================

    /**
     * 签发时间（毫秒时间戳）
     * 对应 JWT 的 iat (Issued At) claim
     *
     * 🎓 用途：
     * - 审计：记录 Token 签发时间
     * - 监控：分析 Token 平均有效期
     * - 安全策略：根据签发时间判断是否需要强制重新登录
     */
    private Long iat;

    /**
     * 过期时间（毫秒时间戳）
     * 对应 JWT 的 exp (Expiration) claim
     *
     * 🎓 用途：
     * - 前端提前刷新：exp - now < 5分钟时刷新
     * - 剩余时间计算：exp - now
     * - 日志分析：记录 Token 过期情况
     */
    private Long exp;

    /**
     * 签发者
     * 对应 JWT 的 iss (Issuer) claim
     *
     * 🎓 示例：
     * - "typetype-server"
     * - "https://api.typetype.com"
     */
    private String iss;

    /**
     * 主题
     * 对应 JWT 的 sub (Subject) claim
     *
     * 🎓 通常是：
     * - 用户 ID（本项目设置）
     * - 用户名
     * - 邮箱
     */
    private String sub;

    /**
     * 计算 Token 剩余有效时间（秒）
     *
     * 🎓 计算公式：
     * remaining = (exp - System.currentTimeMillis()) / 1000
     *
     * @return 剩余秒数，已过期返回负数
     */
    public long getRemainingSeconds() {
        if (exp == null) {
            return 0;
        }
        return (exp - System.currentTimeMillis()) / 1000;
    }

    /**
     * 检查 Token 是否即将过期
     *
     * @param thresholdSeconds 阈值（秒）
     * @return 是否即将过期
     */
    public boolean isExpiringSoon(int thresholdSeconds) {
        return getRemainingSeconds() <= thresholdSeconds;
    }
}
