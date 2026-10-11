/*
 * BlogDocumentPage：博客模块的请求与传输数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.blog.api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 模块间文档分页。
 *
 * 用游标（最后一条的 ID）而不是 offset：重建索引是长流程，
 * 期间新发布文章会让 offset 分页漏读或重读，游标不会。
 */
/**
 * 博客文档分页，属于博客模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogDocumentPage {

    /**
     * 条目集合。
     */
    private List<BlogPostDocument> items;

    // 下一页游标，null 表示已经到末尾
    /**
     * 下一项Cursor。
     */
    private Long nextCursor;
}
