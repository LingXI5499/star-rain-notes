package com.starrainnotes.site.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 首页「最近更新」的一条混合内容项。
 *
 * 教程、博客、作品三种内容在各自模块里的 VO 字段并不一致（章节数、标签、封面……首页列表都不需要），
 * 因此聚合时先收敛成这一种投影，再由前端按 V1 首页的列表样式渲染。
 * `routePath` 由 Site 负责拼装，前端不需要知道各模块的详情页路由规则。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeLatestItemVO {
    private String type;
    private Long id;
    private String title;
    private String summary;
    private String routePath;
    private LocalDateTime publishedAt;
}
