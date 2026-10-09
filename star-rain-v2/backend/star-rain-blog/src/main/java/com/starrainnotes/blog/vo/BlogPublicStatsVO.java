package com.starrainnotes.blog.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 博客首页右栏的「写作统计」。
 *
 * 只有三项，全部来自已发布文章，刻意不含访问量/访客数：
 *   postCount        已发布文章数
 *   wordCount        已发布正文的字符数合计（CHAR_LENGTH，中文按字算）
 *   firstPublishedAt 最早一篇的发布时间 —— 前端用它算「开始写作 / 已写 N 天」
 *
 * 不建「建站日期」配置，而是取 MIN(published_at)：
 * 站点上线的真实含义就是第一篇文章出现的那天，多一个配置项只会和文章数据对不上。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPublicStatsVO {

    private long postCount;
    private long wordCount;
    private LocalDateTime firstPublishedAt;
}
