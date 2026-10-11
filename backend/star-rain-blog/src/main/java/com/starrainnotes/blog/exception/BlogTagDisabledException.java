package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 标签已停用，不能再绑定到文章，也不能作为发布前的新绑定。
 *
 * 注意：停用不会删除历史绑定，所以“已绑定的停用标签”不影响文章继续发布，
 * 只有“本次要新绑定的标签是停用状态”才报这个错误。
 * 错误码 BLOG_TAG_DISABLED，HTTP 409。
 */
public class BlogTagDisabledException extends ApiException {

    public BlogTagDisabledException() {
        super("BLOG_TAG_DISABLED", "标签已停用，不能绑定到文章", 409);
    }
}
