package com.starrainnotes.english.shared.bundle.dto;

import java.util.List;
import java.util.Map;

public record BundleReadinessView(
        int totalItems,
        int publishedItems,
        int moduleCount,
        Map<String, Integer> moduleCounts,
        boolean ready,
        List<String> issues) {
}
