package com.starrainnotes.blog.service;

import com.starrainnotes.blog.dto.BlogTagDTO;
import com.starrainnotes.blog.dto.BlogTagQueryDTO;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.common.result.PageResult;
import java.util.List;

/*
 * BLOG-004 标签管理。
 *
 * Tag 的语义是“多维分类”：一篇文章可以有多个，彼此无序，靠名字检索。
 * 需要顺序的策展场景请用 Topic，不要给 Tag 加排序字段。
 */
public interface BlogTagService {

    PageResult<BlogTagVO> page(BlogTagQueryDTO query);

    // 前台可用的标签集合：只含 ENABLED 且至少有一篇已发布文章的标签
    List<BlogTagVO> listPublished();

    BlogTagVO create(BlogTagDTO request);

    BlogTagVO update(Long tagId, BlogTagDTO request);

    // 停用：不再接受新绑定，但保留已有绑定关系
    void disable(Long tagId);

    void enable(Long tagId);
}
