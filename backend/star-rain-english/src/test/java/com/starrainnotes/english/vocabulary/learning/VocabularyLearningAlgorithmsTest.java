package com.starrainnotes.english.vocabulary.learning;

import static org.junit.jupiter.api.Assertions.*;
import com.starrainnotes.english.vocabulary.learning.VocabularyLearningModels.ModeMemory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class VocabularyLearningAlgorithmsTest {
    private final LocalDateTime first = LocalDateTime.of(2026, 10, 1, 0, 0);
    private List<ModeMemory> goodModes(int count) {
        List<ModeMemory> result = new ArrayList<>();
        for (String direction : VocabularyMasteryCalculator.DIRECTIONS) {
            ModeMemory mode = new ModeMemory(); mode.setDirection(direction); mode.setRatingCount(count);
            mode.setEmaScore(100); mode.setLastRating("KNOW"); mode.setAudioVerified(true); result.add(mode);
        }
        return result;
    }
    @Test void emptyAndFirstRatingsUseConfidenceAndNeverPromoteLegacyCounts() {
        assertEquals(0, VocabularyMasteryCalculator.score(List.of()));
        assertNull(VocabularyMasteryCalculator.rank(0, false, false));
        assertEquals(33, VocabularyMasteryCalculator.score(goodModes(1)));
        assertEquals(67, VocabularyMasteryCalculator.score(goodModes(2)));
        assertEquals(100, VocabularyMasteryCalculator.score(goodModes(3)));
        assertEquals("SKILLED", VocabularyMasteryCalculator.rank(100, false, true));
    }
    @Test void emaFallsOnForgottenRecall() {
        assertEquals(60, VocabularyMasteryCalculator.updatedEma(0, 100, "UNCERTAIN"));
        assertEquals(40, VocabularyMasteryCalculator.updatedEma(3, 100, "FORGOT"));
        assertEquals(76, VocabularyMasteryCalculator.updatedEma(4, 40, "KNOW"));
    }
    @Test void graduationRequiresEachModeAudioAndRealSiteDays() {
        var modes = goodModes(3); var now = first.plusDays(7);
        assertTrue(VocabularyMasteryCalculator.graduated(modes, first, now, 3, 2));
        assertFalse(VocabularyMasteryCalculator.graduated(modes, first, first, 1, 1));
        assertFalse(VocabularyMasteryCalculator.graduated(modes, first, now, 2, 2));
        assertFalse(VocabularyMasteryCalculator.graduated(modes, first, now, 3, 1));
        modes.get(2).setAudioVerified(false);
        assertFalse(VocabularyMasteryCalculator.graduated(modes, first, now, 3, 2));
        modes.get(2).setAudioVerified(true); modes.get(0).setLastRating("UNCERTAIN");
        assertFalse(VocabularyMasteryCalculator.graduated(modes, first, now, 3, 2));
    }
    @Test void threeModesAreRequiredAndOneMissingCannotGraduate() {
        var modes = goodModes(100);
        assertEquals(67, VocabularyMasteryCalculator.score(modes.subList(0, 2)));
        assertFalse(VocabularyMasteryCalculator.graduated(modes.subList(0, 2), first, first.plusDays(7), 3, 2));
        modes.getFirst().setRatingCount(2);
        assertFalse(VocabularyMasteryCalculator.graduated(modes, first, first.plusDays(7), 3, 2));
    }
    @Test void initialRatingStageAndBoundaryCaps() {
        for (String rating : List.of("KNOW", "UNCERTAIN", "FORGOT")) {
            assertEquals(0, VocabularyHighIntensityReviewPolicy.nextStep(-1, rating, false));
        }
        assertEquals(1, VocabularyHighIntensityReviewPolicy.nextStep(0, "KNOW", false));
        assertEquals(0, VocabularyHighIntensityReviewPolicy.nextStep(1, "UNCERTAIN", false));
        assertEquals(8, VocabularyHighIntensityReviewPolicy.nextStep(10, "UNCERTAIN", false));
        assertEquals(13, VocabularyHighIntensityReviewPolicy.nextStep(13, "KNOW", false));
        assertEquals(28L * 86400, VocabularyHighIntensityReviewPolicy.intervalSeconds(13));
        assertEquals(300, VocabularyHighIntensityReviewPolicy.intervalSeconds(0));
    }
    @Test void graduationConsolidatesAndFailureImmediatelyExits() {
        assertEquals(14, VocabularyHighIntensityReviewPolicy.nextStep(13, "KNOW", true));
        assertEquals(14, VocabularyHighIntensityReviewPolicy.nextStep(14, "KNOW", true));
        assertEquals(35L * 86400, VocabularyHighIntensityReviewPolicy.intervalSeconds(14));
        assertEquals(2, VocabularyHighIntensityReviewPolicy.nextStep(14, "UNCERTAIN", false));
        assertEquals(0, VocabularyHighIntensityReviewPolicy.nextStep(14, "FORGOT", false));
        assertEquals(13, VocabularyHighIntensityReviewPolicy.nextStep(14, "KNOW", false));
    }
    @Test void siteDayUsesShanghaiMidnightAndLeapDay() {
        assertEquals("2024-02-29", VocabularyMasteryCalculator.siteDay(LocalDateTime.of(2024, 2, 28, 16, 0)).toString());
        assertEquals("2026-10-09", VocabularyMasteryCalculator.siteDay(LocalDateTime.of(2026, 10, 8, 16, 0)).toString());
        var beforeMidnight = LocalDateTime.of(2026, 10, 1, 15, 59);
        assertTrue(VocabularyMasteryCalculator.graduated(goodModes(3), beforeMidnight, beforeMidnight.plusDays(7), 3, 2));
        assertFalse(VocabularyMasteryCalculator.graduated(goodModes(3), beforeMidnight, beforeMidnight.plusDays(6), 3, 2));
    }
    @Test void earlyOnTimeAndOverdueDoNotSkipStages() {
        assertEquals("NEW", VocabularyHighIntensityReviewPolicy.timing(null, first));
        assertEquals("EARLY", VocabularyHighIntensityReviewPolicy.timing(first.plusMinutes(1), first));
        assertEquals("ON_TIME", VocabularyHighIntensityReviewPolicy.timing(first, first.plusMinutes(5)));
        assertEquals("OVERDUE", VocabularyHighIntensityReviewPolicy.timing(first, first.plusDays(3)));
        assertEquals(1, VocabularyHighIntensityReviewPolicy.nextStep(0, "KNOW", false));
    }
}
