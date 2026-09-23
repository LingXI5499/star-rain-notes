package com.starrainnotes.english.learning.domain;

import com.starrainnotes.english.learning.dto.LearningRecommendationView;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Preserves the current source precedence, deduplication and priority ordering. */
@Component
public class RecommendationEngine {
    private static final int LIMIT = 8;

    public List<LearningRecommendationView> select(
            List<LearningRecommendationView> reviewAndContinue,
            List<LearningRecommendationView> bundleNext,
            List<LearningRecommendationView> paired,
            List<LearningRecommendationView> tagMatches,
            Supplier<List<LearningRecommendationView>> starters) {
        LinkedHashMap<Key, LearningRecommendationView> result = new LinkedHashMap<>();
        add(result, reviewAndContinue);
        add(result, bundleNext);
        add(result, paired);
        add(result, tagMatches);
        if (result.size() < 4) add(result, starters.get());
        return result.values().stream()
                .sorted(Comparator.comparingInt(LearningRecommendationView::priority))
                .limit(LIMIT)
                .toList();
    }

    private void add(Map<Key, LearningRecommendationView> result, List<LearningRecommendationView> candidates) {
        for (LearningRecommendationView candidate : candidates) {
            result.putIfAbsent(new Key(candidate.contentType(), candidate.contentId()), candidate);
            if (result.size() >= LIMIT) return;
        }
    }

    private record Key(String type, Long id) { }
}
