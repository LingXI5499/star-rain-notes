package com.starrainnotes.blog.dto;

/** Admin tag option with the number of posts currently using it. */
public record AdminBlogTagView(
        Long id,
        String name,
        String slug,
        long postCount) {
}
