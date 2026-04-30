package com.typetype.score.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩实体（V2 指标集）
 *
 * 🎓 设计原则：
 * - 只存储原始字段：由客户端直接采集
 * - 派生字段（speed, keyStroke, codeLength, keyAccuracy）由计算层动态生成
 *
 * 💡 原始字段：
 * - charCount, wrongCharCount：字符统计
 * - backspaceCount, correctionCount：按键统计
 * - keyStrokeCount：总按键次数
 * - time：用时（秒）
 *
 * 💡 索引设计：
 * - idx_user_created：用户历史记录查询（DESC 支持最近优先）
 * - idx_created_at：时间范围查询
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
     * 字符数（原始字段）
     */
    private Integer charCount;

    /**
     * 错误字符数（原始字段）
     */
    private Integer wrongCharCount;

    /**
     * 退格键按下次数（原始字段）
     */
    private Integer backspaceCount;

    /**
     * 回改字数（原始字段）
     */
    private Integer correctionCount;

    /**
     * 总按键次数（原始字段）
     */
    private Integer keyStrokeCount;

    /**
     * 用时（秒）（原始字段）
     */
    private BigDecimal time;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 文本标题（关联查询字段，非数据库字段）
     */
    private String textTitle;

    // ========== 派生字段计算方法 ==========

    /**
     * 速度（字/分）= charCount * 60 / time
     */
    public BigDecimal getSpeed() {
        if (time == null || time.compareTo(BigDecimal.ZERO) == 0 || charCount == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(charCount * 60.0).divide(time, 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 击键速度（击/秒）= keyStrokeCount / time
     */
    public BigDecimal getKeyStroke() {
        if (time == null || time.compareTo(BigDecimal.ZERO) == 0 || keyStrokeCount == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(keyStrokeCount).divide(time, 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 码长（击/字）= keyStrokeCount / charCount
     */
    public BigDecimal getCodeLength() {
        if (charCount == null || charCount == 0 || keyStrokeCount == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(keyStrokeCount).divide(BigDecimal.valueOf(charCount), 3, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 键准（%）= (keyStrokeCount - wrongKeys) / keyStrokeCount * 100
     * wrongKeys = backspaceCount + correctionCount * codeLength
     */
    public BigDecimal getKeyAccuracy() {
        if (keyStrokeCount == null || keyStrokeCount == 0) {
            return BigDecimal.valueOf(100.0);
        }
        BigDecimal codeLen = getCodeLength();
        int backspace = backspaceCount != null ? backspaceCount : 0;
        int correction = correctionCount != null ? correctionCount : 0;
        double wrongKeys = backspace + correction * codeLen.doubleValue();
        double accuracy = Math.max(0.0, (keyStrokeCount - wrongKeys) / keyStrokeCount * 100.0);
        return BigDecimal.valueOf(accuracy);
    }

    /**
     * 有效速度（字/分）= speed * (correctChars / charCount)
     * 直接通过原始字段计算，不依赖 accuracyRate
     */
    public BigDecimal getEffectiveSpeed() {
        if (time == null || time.compareTo(BigDecimal.ZERO) == 0 || charCount == null) {
            return BigDecimal.ZERO;
        }
        int correctChars = charCount - (wrongCharCount != null ? wrongCharCount : 0);
        return BigDecimal.valueOf(correctChars * 60.0).divide(time, 2, java.math.RoundingMode.HALF_UP);
    }
}
