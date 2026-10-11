/*
 * BlogTopicOrderDTO：博客模块的请求与传输数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

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
/**
 * 博客专题顺序传输对象，属于博客模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class BlogTopicOrderDTO {

    /**
     * 文章标识集合。
     */
    private List<Long> postIds;
}
