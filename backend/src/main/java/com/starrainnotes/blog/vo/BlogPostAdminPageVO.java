package com.starrainnotes.blog.vo;

import java.util.List;

public record BlogPostAdminPageVO(List<BlogPostAdminSummaryVO> items, int page, int pageSize, long total,
                                  int totalPages) {
}
