/*
 * BlogPostUpdateDTO：博客模块的请求与传输数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.blog.dto;

import java.util.List;
import lombok.Data;

/*
 * BLOG-003 修改文章元数据请求（PATCH 语义）。
 *
 * null 表示“本次不改这个字段”，因此不能靠把 coverMediaAssetId 置 null 来取消封面：
 * 那个语义由 clearCover 显式表达，否则前端漏传一个字段就会静默清掉封面。
 * tagIds 同理：null 不改绑定，空数组表示解绑全部标签。
 */
/**
 * 博客文章更新传输对象，属于博客模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class BlogPostUpdateDTO {

    /**
     * 标题。
     */
    private String title;
    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 摘要。
     */
    private String summary;

    /**
     * 封面媒体资产标识。
     */
    private Long coverMediaAssetId;

    // 显式取消封面，与“未提供封面字段”区分开
    /**
     * clear封面。
     */
    private Boolean clearCover;

    /**
     * 标签标识集合。
     */
    private List<Long> tagIds;
}
