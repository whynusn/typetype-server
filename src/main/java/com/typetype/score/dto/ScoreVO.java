package com.typetype.score.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩详情 VO
 *
 * 🎓 学习点：
 * - 用于用户历史成绩列表展示
 * - 包含关联的文本标题信息
 */
@Data
@Builder
public class ScoreVO {

    /**
     * 成绩ID
     */
    private Long id;

    /**
     * 文本ID
     */
    private Long textId;

    /**
     * 文本标题
     */
    private String textTitle;

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
     * 创建时间
     */
    private LocalDateTime createdAt;
}
