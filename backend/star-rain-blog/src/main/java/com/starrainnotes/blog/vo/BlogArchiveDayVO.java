package com.starrainnotes.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 指定月份内已发布文章的日期桶，给归档日历显示每天篇数。
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogArchiveDayVO {

    private Integer day;
    private Long postCount;
}
