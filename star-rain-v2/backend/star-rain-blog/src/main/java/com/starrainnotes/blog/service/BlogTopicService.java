package com.starrainnotes.blog.service;

import com.starrainnotes.blog.dto.BlogTopicDTO;
import com.starrainnotes.blog.dto.BlogTopicMemberRow;
import com.starrainnotes.blog.dto.BlogTopicQueryDTO;
import com.starrainnotes.blog.vo.BlogTopicVO;
import com.starrainnotes.common.result.PageResult;
import java.util.List;

/*
 * BLOG-005 / BLOG-006 / BLOG-007 专题管理与专题内排序。
 *
 * Topic 的语义是“人工策展的有序专题”：加入顺序由人决定，且顺序本身是内容的一部分。
 * 因此这里比 Tag 多了成员管理、sortOrder 与整体重排三类操作。
 */
public interface BlogTopicService {

    PageResult<BlogTopicVO> page(BlogTopicQueryDTO query);

    // 前台专题入口：只含 ENABLED 且至少有一篇已发布文章的专题
    List<BlogTopicVO> listPublished();

    BlogTopicVO create(BlogTopicDTO request);

    BlogTopicVO update(Long topicId, BlogTopicDTO request);

    // 停用：不在前台展示，但成员与顺序全部保留
    void disable(Long topicId);

    void enable(Long topicId);

    // 专题成员（按 sortOrder 升序），后台排序界面用
    List<BlogTopicMemberRow> members(Long topicId);

    // BLOG-006 加入：追加到末尾
    void addPost(Long topicId, Long postId);

    // BLOG-006 移出：移除后压缩序号，保持 1..n 连续
    void removePost(Long topicId, Long postId);

    // BLOG-007 重排：要求 postIds 与当前成员集合完全一致，序号按下标重写为 1..n
    void reorder(Long topicId, List<Long> postIds);
}
