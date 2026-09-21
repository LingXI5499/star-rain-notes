package com.starrainnotes.blog.vo;

import java.util.List;

public record BlogPostAdminDetailVO(Long id, String title, String slug, String summary, String bodyMarkdown,
                                    Long coverMediaId, String coverUrl, String publishStatus, String seoTitle,
                                    String seoDescription, String publishedAt, String createdAt, String updatedAt,
                                    List<BlogTagVO> tags) {
}
