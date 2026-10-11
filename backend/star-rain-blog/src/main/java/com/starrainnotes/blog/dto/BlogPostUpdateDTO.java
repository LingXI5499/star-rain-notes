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
@Data
public class BlogPostUpdateDTO {

    private String title;
    private String slug;
    private String summary;

    private Long coverMediaAssetId;

    // 显式取消封面，与“未提供封面字段”区分开
    private Boolean clearCover;

    private List<Long> tagIds;
}
