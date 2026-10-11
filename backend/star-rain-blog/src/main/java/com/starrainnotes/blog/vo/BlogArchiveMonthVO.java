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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogArchiveMonthVO {

    private Integer year;
    private Integer month;
    private Long postCount;
}
