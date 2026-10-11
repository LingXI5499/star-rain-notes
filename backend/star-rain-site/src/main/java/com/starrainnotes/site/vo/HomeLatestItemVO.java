/*
 * HomeLatestItemVO：站点模块的响应数据文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

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
/**
 * 首页最新Item响应对象，属于站点模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeLatestItemVO {
    /**
     * 类型。
     */
    private String type;
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 标题。
     */
    private String title;
    /**
     * 摘要。
     */
    private String summary;
    /**
     * 路由路径。
     */
    private String routePath;
    /**
     * 已发布时间。
     */
    private LocalDateTime publishedAt;
}
