package com.typetype.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息 VO（返回给前端）
 *
 * 🎓 学习点：
 * - VO（View Object）：专门用于返回给前端
 * - 不包含 password 字段，避免敏感信息泄露
 * - 字段命名采用驼峰（userId 而非 user_id）
 *
 * 💡 安全性：
 * - Entity.User 包含 password 字段
 * - UserVO 不包含 password
 * - 通过 Entity → VO 转换实现数据过滤
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
