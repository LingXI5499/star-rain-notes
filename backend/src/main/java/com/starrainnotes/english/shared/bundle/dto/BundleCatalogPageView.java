package com.starrainnotes.english.shared.bundle.dto;

import java.util.List;

public record BundleCatalogPageView(
        List<BundleCatalogItemView> items,
        int page,
        int pageSize,
        long total,
        int totalPages) {
}
