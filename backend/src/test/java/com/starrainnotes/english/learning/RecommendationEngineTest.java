package com.starrainnotes.english.learning;

import com.starrainnotes.english.learning.domain.RecommendationEngine;
import com.starrainnotes.english.learning.dto.LearningRecommendationView;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationEngineTest {
    private final RecommendationEngine engine = new RecommendationEngine();

    @Test
    void keepsSourcePrecedenceWhenContentAppearsTwice() {
        AtomicBoolean startersQueried = new AtomicBoolean();
        List<LearningRecommendationView> result = engine.select(
                List.of(item("READING", 1, "REVIEW", 10), item("GRAMMAR", 2, "CONTINUE", 20)),
                List.of(item("READING", 1, "BUNDLE_NEXT", 30), item("LISTENING", 3, "BUNDLE_NEXT", 30)),
                List.of(item("WRITING", 4, "PAIRED", 40)),
                List.of(item("READING", 5, "TAG_MATCH", 50)),
                () -> {
                    startersQueried.set(true);
                    return List.of(item("GRAMMAR", 6, "STARTER", 60));
                });

        assertThat(result).extracting(LearningRecommendationView::recommendationType)
                .containsExactly("REVIEW", "CONTINUE", "BUNDLE_NEXT", "PAIRED", "TAG_MATCH");
        assertThat(startersQueried).isFalse();
    }

    @Test
    void fillsSmallResultsWithStartersAndLimitsOutput() {
        List<LearningRecommendationView> small = engine.select(
                List.of(item("READING", 1, "REVIEW", 10)), List.of(), List.of(), List.of(),
                () -> List.of(item("GRAMMAR", 2, "STARTER", 60)));
        assertThat(small).extracting(LearningRecommendationView::recommendationType)
                .containsExactly("REVIEW", "STARTER");

        List<LearningRecommendationView> many = engine.select(
                java.util.stream.LongStream.rangeClosed(1, 10)
                        .mapToObj(id -> item("READING", id, "REVIEW", 10)).toList(),
                List.of(), List.of(), List.of(), List::of);
        assertThat(many).hasSize(8);
    }

    private LearningRecommendationView item(String type, long id, String recommendationType, int priority) {
        return new LearningRecommendationView(type, id, "slug-" + id, "Title " + id,
                "/english/" + type.toLowerCase() + "/slug-" + id,
                recommendationType, null, null, null, recommendationType, priority, null);
    }
}
