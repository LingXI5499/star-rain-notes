/*
 * BlogTopicMemberRow：博客模块的请求与传输数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.blog.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 专题成员行：后台专题排序界面用。
 *
 * 带上文章状态与发布时间，是因为草稿也可能先被排进专题，
 * 界面上必须能看出“这条还没发布”，否则排序结果与前台看到的不一致时无从排查。
 */
/**
 * 博客专题Member行，属于博客模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogTopicMemberRow {

    /**
     * 文章标识。
     */
    private Long postId;
    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 标题。
     */
    private String title;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 展示排序值。
     */
    private Integer sortOrder;
    /**
     * 已发布时间。
     */
    private LocalDateTime publishedAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
