package com.typetype.common.result;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页响应包装类
 *
 * 🎓 学习点：
 * - 专门用于包装分页查询的结果
 * - 配合 Result<T> 使用：Result<PageResult<User>>
 *
 * 💡 使用场景：
 * - 查询用户列表：返回 PageResult<User>
 * - 查询成绩历史：返回 PageResult<Score>
 * - 查询排行榜：返回 PageResult<RankingVO>
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Long page;

    /**
     * 每页大小
     */
    private Long size;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 创建分页结果（自动计算总页数）
     *
     * @param records 数据列表
     * @param total 总记录数
     * @param page 当前页
     * @param size 每页大小
     * @param <T> 数据类型
     * @return PageResult<T>
     */
    public static <T> PageResult<T> of(
        List<T> records,
        long total,
        long page,
        long size
    ) {
        long pages = (total + size - 1) / size; // 向上取整
        return PageResult.<T>builder()
            .records(records)
            .total(total)
            .page(page)
            .size(size)
            .pages(pages)
            .build();
    }
}
