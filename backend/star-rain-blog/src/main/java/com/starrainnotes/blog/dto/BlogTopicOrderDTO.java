package com.starrainnotes.blog.dto;

import java.util.List;
import lombok.Data;

/*
 * BLOG-007 调整专题内顺序请求。
 *
 * 只收 postIds，不收 (postId, sortOrder) 对：顺序是前端拖拽结果的完整视图，
 * 让调用方同时给出序号会引入“序号重复 / 有空洞 / 与列表不一致”三类新问题。
 * Service 按数组下标重排为 1..n，并要求集合与当前成员完全一致。
 */
@Data
public class BlogTopicOrderDTO {

    private List<Long> postIds;
}
