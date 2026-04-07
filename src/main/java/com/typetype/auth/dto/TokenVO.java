package com.typetype.auth.dto;

import com.typetype.user.dto.UserVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功后返回的 Token 信息
 *
 * 🎓 学习点：
 * - JWT 双 Token 机制：
 *   - accessToken：访问令牌，有效期短（如 15 分钟）
 *   - refreshToken：刷新令牌，有效期长（如 7 天）
 *   - 当 accessToken 过期时，用 refreshToken 换取新的 accessToken
 *
 * 💡 Token Rotation（令牌轮换）：
 * - 刷新 Token 时生成新的 refreshToken
 * - 旧的 refreshToken 立即失效
 * - 防止 Token 重放攻击
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenVO {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 访问令牌过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户信息
     */
    private UserVO user;
}
