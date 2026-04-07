package com.typetype.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体
 *
 * 🎓 学习点：
 * - Entity 对应数据库表 t_user
 * - 字段命名遵循驼峰规则（数据库下划线自动映射）
 * - @AllArgsConstructor(access = AccessLevel.PROTECTED)：限制构造器，强制使用 Builder
 *
 * 💡 安全提示：
 * - password 字段敏感，不应直接暴露给前端
 * - 使用 UserVO 替换，不包含 password
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class User {

    /**
     * 用户ID（主键）
     */
    private Long id;

    /**
     * 用户名（唯一）
     */
    private String username;

    /**
     * 密码（BCrypt 加密）
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 角色（如 USER / ADMIN）
     */
    private String role;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
