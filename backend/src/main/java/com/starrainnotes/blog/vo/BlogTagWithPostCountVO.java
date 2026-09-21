package com.starrainnotes.blog.vo;

/** A tag and the count of posts visible to the calling endpoint. */
public record BlogTagWithPostCountVO(Long id, String name, String slug, long postCount) {
}
