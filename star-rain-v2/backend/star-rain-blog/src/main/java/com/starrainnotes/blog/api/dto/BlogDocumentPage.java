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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogDocumentPage {

    private List<BlogPostDocument> items;

    // 下一页游标，null 表示已经到末尾
    private Long nextCursor;
}
