package com.starrainnotes.blog.vo;

import java.util.List;

public record BlogPostPublicPageVO(List<BlogPostPublicSummaryVO> items, int page, int pageSize, long total,
                                   int totalPages) {
}
