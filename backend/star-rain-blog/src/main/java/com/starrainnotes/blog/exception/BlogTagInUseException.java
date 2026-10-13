package com.starrainnotes.blog.exception;

import com.starrainnotes.common.exception.ApiException;

/** Preserve existing article bindings rather than cascading a taxonomy deletion. */
public class BlogTagInUseException extends ApiException {
    /** A referenced tag cannot be physically deleted. */
    public BlogTagInUseException() {
        super("BLOG_TAG_IN_USE", "标签仍被文章引用，请先解除全部关联或使用停用", 409);
    }
}
