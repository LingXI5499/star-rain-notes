/*
 * BlogTopicDetailVO：博客模块的响应数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 * 主要协作类型：PageResult。
 */

package com.starrainnotes.blog.vo;

import com.starrainnotes.common.result.PageResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 前台专题页：专题本身 + 该专题下的文章分页。
 *
 * 两者必须一起返回。分两个接口拿会让页面出现「标题已经是专题 A、列表还是专题 B」
 * 的中间态，而且前端还要自己保证两次请求的参数一致。
 *
 * posts 里的顺序是策展顺序（sr_blog_topic_post.sort_order），不是发布时间倒序 ——
 * 这正是专题与标签的区别，前端按数组顺序渲染即可，不要再排一次。
 */
/**
 * 博客专题详情响应对象，属于博客模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogTopicDetailVO {

    /**
     * 专题。
     */
    private BlogTopicVO topic;
    /**
     * 文章列表。
     */
    private PageResult<BlogPostPublicVO> posts;
}
