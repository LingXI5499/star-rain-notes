package com.starrainnotes.blog.vo;

import java.util.List;

public record BlogPostPublicSummaryVO(Long id, String title, String slug, String summary, String coverUrl,
                                      String publishedAt, String updatedAt, List<BlogTagVO> tags) {
}
