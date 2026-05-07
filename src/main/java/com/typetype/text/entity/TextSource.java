package com.typetype.text.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文本来源实体
 *
 * 🎓 学习点：
 * - 枚举字段 category：vocabulary/article/custom
 * - is_active 布尔字段映射：数据库 TINYINT(1) → Java Boolean
 *
 * 💡 使用场景：
 * - 客户端展示"文本来源"下拉选择框
 * - 随机获取文本时按 sourceKey 筛选
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class TextSource implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 来源ID（主键）
     */
    private Long id;

    /**
     * 来源标识（如 cet4、essay_classic，唯一）
     */
    private String sourceKey;

    /**
     * 来源名称（如 "CET-4 词汇"）
     */
    private String label;

    /**
     * 分类（vocabulary/article/custom）
     */
    private String category;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
