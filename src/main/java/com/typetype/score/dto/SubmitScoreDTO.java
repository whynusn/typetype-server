package com.typetype.score.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 提交成绩请求 DTO
 *
 * 🎓 学习点：
 * - @NotNull/@DecimalMin 等校验注解配合 @Valid 使用
 * - BigDecimal 用于精确数值类型
 */
@Data
public class SubmitScoreDTO {

    /**
     * 文本ID
     */
    @NotNull(message = "文本ID不能为空")
    private Long textId;

    /**
     * 速度（字/分）
     */
    @NotNull(message = "速度不能为空")
    @DecimalMin(value = "0", message = "速度不能为负数")
    private BigDecimal speed;

    /**
     * 有效速度（字/分）
     */
    @NotNull(message = "有效速度不能为空")
    @DecimalMin(value = "0", message = "有效速度不能为负数")
    private BigDecimal effectiveSpeed;

    /**
     * 击键速度（击/秒）
     */
    @NotNull(message = "击键速度不能为空")
    @DecimalMin(value = "0", message = "击键速度不能为负数")
    private BigDecimal keyStroke;

    /**
     * 码长（击/字）
     */
    @NotNull(message = "码长不能为空")
    @DecimalMin(value = "0", message = "码长不能为负数")
    private BigDecimal codeLength;

    /**
     * 准确率（%）
     */
    @NotNull(message = "准确率不能为空")
    @DecimalMin(value = "0", message = "准确率不能为负数")
    @DecimalMax(value = "100", message = "准确率不能超过100")
    private BigDecimal accuracyRate;

    /**
     * 字符数
     */
    @NotNull(message = "字符数不能为空")
    @Min(value = 0, message = "字符数不能为负数")
    private Integer charCount;

    /**
     * 错误字符数
     */
    @NotNull(message = "错误字符数不能为空")
    @Min(value = 0, message = "错误字符数不能为负数")
    private Integer wrongCharCount;

    /**
     * 时长（秒）
     */
    @NotNull(message = "时长不能为空")
    @DecimalMin(value = "0", message = "时长不能为负数")
    private BigDecimal duration;
}
