package com.starrainnotes.blog.vo;

import java.util.List;

public record BlogPostPublicDetailVO(Long id, String title, String slug, String summary, String bodyMarkdown,
                                     String coverUrl, List<BlogTagVO> tags, String seoTitle, String seoDescription,
                                     String publishedAt, String updatedAt, BlogPostNeighborVO previous,
                                     BlogPostNeighborVO next) {
}
