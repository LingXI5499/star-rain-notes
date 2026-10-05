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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogTopicDetailVO {

    private BlogTopicVO topic;
    private PageResult<BlogPostPublicVO> posts;
}
