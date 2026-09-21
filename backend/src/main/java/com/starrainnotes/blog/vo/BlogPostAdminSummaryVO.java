package com.starrainnotes.blog.vo;

import java.util.List;

public record BlogPostAdminSummaryVO(Long id, String title, String slug, String summary, String coverUrl,
                                     String publishStatus, String publishedAt, String updatedAt,
                                     List<BlogTagVO> tags) {
}
