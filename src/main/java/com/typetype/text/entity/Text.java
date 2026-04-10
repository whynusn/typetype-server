package com.typetype.text.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文本实体
 *
 * 🎓 学习点：
 * - 外键字段 sourceId：关联 t_text_source
 * - 冗余字段 charCount：避免每次计算 LENGTH(content)
 * - difficulty 难度等级：0-5 分级
 *
 * 💡 性能优化：
 * - charCount 冗余存储：避免运行时计算
 * - 索引 idx_source_difficulty：按来源+难度快速筛选
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Text {

    /**
     * 文本ID（主键）
     */
    private Long id;

    /**
     * 来源ID（外键，关联 t_text_source）
     */
    private Long sourceId;

    /**
     * 文本标题
     */
    private String title;

    /**
     * 文本内容
     */
    private String content;

    /**
     * 字符数（冗余字段）
     */
    private Integer charCount;

    /**
     * 难度等级（0-5）
     */
    private Integer difficulty;

    /**
     * 客户端文本ID（hash值，由客户端基于 label + content 生成）
     */
    private Long clientTextId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
