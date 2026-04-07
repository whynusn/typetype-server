package com.typetype.score.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩实体
 *
 * 🎓 学习点：
 * - 外键字段 userId 和 textId：关联用户和文本
 * - DECIMAL 类型用于精确数值：speed、accuracyRate 等
 * - duration：打字时长（秒）
 *
 * 💡 索引设计：
 * - idx_user_created：用户历史记录查询（DESC 支持最近优先）
 * - idx_speed：排行榜排序（DESC）
 * - idx_created_at：时间范围查询
 *
 * 💡 扩展考虑：
 * - 量大后可按 created_at 按月分区
 * - 或按 userId 做分表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Score {

    /**
     * 成绩ID（主键）
     */
    private Long id;

    /**
     * 用户ID（外键，关联 t_user）
     */
    private Long userId;

    /**
     * 文本ID（外键，关联 t_text，可选）
     */
    private Long textId;

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
     * 准确率（%）
     */
    private BigDecimal accuracyRate;

    /**
     * 字符数
     */
    private Integer charCount;

    /**
     * 错误字符数
     */
    private Integer wrongCharCount;

    /**
     * 时长（秒）
     */
    private BigDecimal duration;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
