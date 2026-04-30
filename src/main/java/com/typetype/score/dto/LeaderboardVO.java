package com.typetype.score.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 排行榜项 VO
 *
 * 🎓 学习点：
 * - VO（View Object）：面向前端展示的数据结构
 * - 只包含前端需要展示的字段
 */
@Data
@Builder
public class LeaderboardVO {

    /**
     * 排名
     */
    private Integer rank;

    /**
     * 用户ID
     */
    private Long userId;

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
     * 速度（字/分）
     */
    private BigDecimal speed;

    /**
     * 有效速度（字/分）
     */
    private BigDecimal effectiveSpeed;

    /**
     * 击键速度（击/秒）
     */
    private BigDecimal keyStroke;

    /**
     * 码长（击/字）
     */
    private BigDecimal codeLength;

    /**
     * 字符数
     */
    private Integer charCount;

    /**
     * 错误字符数
     */
    private Integer wrongCharCount;

    /**
     * 键准（%）
     */
    private BigDecimal keyAccuracy;

    /**
     * 退格键按下次数
     */
    private Integer backspaceCount;

    /**
     * 回改字数
     */
    private Integer correctionCount;

    /**
     * 用时（秒）
     */
    private BigDecimal time;

    /**
     * 达成时间
     */
    private LocalDateTime createdAt;
}
