/*
 * BlogArchiveMonthVO：博客模块的响应数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 归档月份桶。
 *
 * 统计依据是 published_at 而不是 created_at：
 * 草稿可能创建于去年、今年才发布，按创建时间归档会让读者找不到刚发的文章。
 */
/**
 * 博客Archive月份响应对象，属于博客模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogArchiveMonthVO {

    /**
     * 年份。
     */
    private Integer year;
    /**
     * 月份。
     */
    private Integer month;
    /**
     * 文章数量。
     */
    private Long postCount;
}
