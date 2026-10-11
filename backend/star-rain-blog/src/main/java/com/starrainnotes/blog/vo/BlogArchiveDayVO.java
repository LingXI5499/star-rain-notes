/*
 * BlogArchiveDayVO：博客模块的响应数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 指定月份内已发布文章的日期桶，给归档日历显示每天篇数。
/**
 * 博客Archive日响应对象，属于博客模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogArchiveDayVO {

    /**
     * 日。
     */
    private Integer day;
    /**
     * 文章数量。
     */
    private Long postCount;
}
