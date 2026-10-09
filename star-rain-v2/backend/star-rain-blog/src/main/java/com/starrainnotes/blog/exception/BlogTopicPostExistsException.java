package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 文章已经在专题里。
 *
 * 重复加入不当作幂等成功：调用方应当知道这次点击没有产生任何顺序变化。
 * 错误码 BLOG_TOPIC_POST_EXISTS，HTTP 409。
 */
public class BlogTopicPostExistsException extends ApiException {

    public BlogTopicPostExistsException() {
        super("BLOG_TOPIC_POST_EXISTS", "文章已经在该专题中", 409);
    }
}
