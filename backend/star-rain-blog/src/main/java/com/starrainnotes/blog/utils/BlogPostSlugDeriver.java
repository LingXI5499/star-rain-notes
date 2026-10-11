package com.starrainnotes.blog.utils;

import com.starrainnotes.blog.constant.BlogLimits;

/* 标题派生的 slug 只在创建时确定；之后改标题不改已有公开地址。 */
public final class BlogPostSlugDeriver {

    private BlogPostSlugDeriver() {
    }

    public static String derive(String title) {
        return BlogSlugDeriver.derive(title, "post", BlogLimits.POST_SLUG_MAX_LENGTH);
    }

    public static String withSuffix(String base, int ordinal) {
        return BlogSlugDeriver.withSuffix(base, ordinal, BlogLimits.POST_SLUG_MAX_LENGTH);
    }
}
