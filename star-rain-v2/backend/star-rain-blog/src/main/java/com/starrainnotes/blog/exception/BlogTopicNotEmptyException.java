package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 专题下还有文章，不能物理删除。
 *
 * Topic 承载人工策展的成员与顺序，因此删除只对**空专题**开放：
 * 有成员时要求调用方先显式移出（移出会压缩序号），
 * 而不是让一次误点把整份策展结果连同关系一起抹掉。
 * 错误码 BLOG_TOPIC_NOT_EMPTY，HTTP 409。
 */
public class BlogTopicNotEmptyException extends ApiException {

    public BlogTopicNotEmptyException() {
        super("BLOG_TOPIC_NOT_EMPTY", "专题下还有文章，请先移出全部文章再删除", 409);
    }
}
