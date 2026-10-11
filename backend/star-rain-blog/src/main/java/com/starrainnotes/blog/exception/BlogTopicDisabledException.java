package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 专题已停用，不能再加入新文章。
 *
 * 停用不删除成员关系、也不清空顺序：重新启用后原排序继续有效。
 * 错误码 BLOG_TOPIC_DISABLED，HTTP 409。
 */
public class BlogTopicDisabledException extends ApiException {

    public BlogTopicDisabledException() {
        super("BLOG_TOPIC_DISABLED", "专题已停用，不能加入文章", 409);
    }
}
