package com.typetype.score.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 提交成绩请求 DTO（V2 纯原始字段合约）
 *
 * 🎓 V2 设计原则：
 * - 客户端只传原始字段，所有派生指标由服务端统一计算
 * - 避免客户端计算逻辑不一致导致数据污染
 *
 * 只有服务端存在的文本才能提交成绩，因此只需要 textId
 */
@Data
public class SubmitScoreDTO {

    /**
     * 文本ID（服务器主键）
     */
    private Long textId;

    /**
     * 字符数（原始字段）
     */
    @NotNull(message = "字符数不能为空")
    @Min(value = 0, message = "字符数不能为负数")
    private Integer charCount;

    /**
     * 错误字符数（原始字段）
     */
    @NotNull(message = "错误字符数不能为空")
    @Min(value = 0, message = "错误字符数不能为负数")
    private Integer wrongCharCount;

    /**
     * 退格键按下次数（原始字段）
     */
    @NotNull(message = "退格数不能为空")
    @Min(value = 0, message = "退格数不能为负数")
    private Integer backspaceCount;

    /**
     * 回改字数（原始字段）
     */
    @NotNull(message = "回改数不能为空")
    @Min(value = 0, message = "回改数不能为负数")
    private Integer correctionCount;

    /**
     * 总按键次数（原始字段）
     */
    @NotNull(message = "总按键次数不能为空")
    @Min(value = 0, message = "总按键次数不能为负数")
    private Integer keyStrokeCount;

    /**
     * 用时（秒）（原始字段）
     */
    @NotNull(message = "用时不能为空")
    @DecimalMin(value = "0", message = "用时不能为负数")
    private BigDecimal time;
}
