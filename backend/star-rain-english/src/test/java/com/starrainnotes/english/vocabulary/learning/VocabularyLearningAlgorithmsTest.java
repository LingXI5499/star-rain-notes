/*
 * VocabularyLearningAlgorithmsTest：英语学习模块的测试文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：ModeMemory。
 * 阅读入口：goodModes（goodModes处理入口）；emptyAndFirstRatingsUseConfidenceAndNeverPromoteLegacyCounts（空白及首次RatingsUseConfidence及NeverPromoteLegacy数量集合处理入口）；emaFallsOnForgottenRecall（emaFalls触发ForgottenRecall处理入口）；graduationRequiresEachModeAudioAndRealSiteDays（graduationRequiresEachMode音频及Real站点Days处理入口）。
 */

package com.starrainnotes.english.vocabulary.learning;

import static org.junit.jupiter.api.Assertions.*;
import com.starrainnotes.english.vocabulary.learning.VocabularyLearningModels.ModeMemory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 词汇学习Algorithms测试，属于英语学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class VocabularyLearningAlgorithmsTest {
    /**
     * 首次。
     */
    private final LocalDateTime first = LocalDateTime.of(2026, 10, 1, 0, 0);
    /**
     * goodModes处理入口。
     * 关键处理步骤调用 <code>mode.setDirection</code>、<code>mode.setRatingCount</code>、<code>mode.setEmaScore</code>、<code>mode.setLastRating</code>。
     *
     * @param count 数量；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
     */
    private List<ModeMemory> goodModes(int count) {
        List<ModeMemory> result = new ArrayList<>();
        for (String direction : VocabularyMasteryCalculator.DIRECTIONS) {
            ModeMemory mode = new ModeMemory(); mode.setDirection(direction); mode.setRatingCount(count);
            mode.setEmaScore(100); mode.setLastRating("KNOW"); mode.setAudioVerified(true); result.add(mode);
        }
        return result;
    }
    /**
     * 空白及首次RatingsUseConfidence及NeverPromoteLegacy数量集合处理入口。
     * 关键处理步骤调用 <code>VocabularyMasteryCalculator.score</code>、<code>List.of</code>、<code>VocabularyMasteryCalculator.rank</code>、<code>goodModes</code>。
     */
    @Test void emptyAndFirstRatingsUseConfidenceAndNeverPromoteLegacyCounts() {
        assertEquals(0, VocabularyMasteryCalculator.score(List.of()));
        assertNull(VocabularyMasteryCalculator.rank(0, false, false));
        assertEquals(33, VocabularyMasteryCalculator.score(goodModes(1)));
        assertEquals(67, VocabularyMasteryCalculator.score(goodModes(2)));
        assertEquals(100, VocabularyMasteryCalculator.score(goodModes(3)));
        assertEquals("SKILLED", VocabularyMasteryCalculator.rank(100, false, true));
    }
    /**
     * emaFalls触发ForgottenRecall处理入口。
     * 关键处理步骤调用 <code>VocabularyMasteryCalculator.updatedEma</code>。
     */
    @Test void emaFallsOnForgottenRecall() {
        assertEquals(60, VocabularyMasteryCalculator.updatedEma(0, 100, "UNCERTAIN"));
        assertEquals(40, VocabularyMasteryCalculator.updatedEma(3, 100, "FORGOT"));
        assertEquals(76, VocabularyMasteryCalculator.updatedEma(4, 40, "KNOW"));
    }
    /**
     * graduationRequiresEachMode音频及Real站点Days处理入口。
     * 主要协作调用：<code>first.plusDays</code>。
     */
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
    /**
     * threeModesAre必需及单次缺失CannotGraduate处理入口。
     * 主要协作调用：<code>first.plusDays</code>。
     */
    @Test void threeModesAreRequiredAndOneMissingCannotGraduate() {
        var modes = goodModes(100);
        assertEquals(67, VocabularyMasteryCalculator.score(modes.subList(0, 2)));
        assertFalse(VocabularyMasteryCalculator.graduated(modes.subList(0, 2), first, first.plusDays(7), 3, 2));
        modes.getFirst().setRatingCount(2);
        assertFalse(VocabularyMasteryCalculator.graduated(modes, first, first.plusDays(7), 3, 2));
    }
    /**
     * initial评分Stage及边界Caps处理入口。
     * 关键处理步骤调用 <code>List.of</code>、<code>VocabularyHighIntensityReviewPolicy.nextStep</code>、<code>VocabularyHighIntensityReviewPolicy.intervalSeconds</code>。
     */
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
    /**
     * graduationConsolidates及失败ImmediatelyExits处理入口。
     * 关键处理步骤调用 <code>VocabularyHighIntensityReviewPolicy.nextStep</code>、<code>VocabularyHighIntensityReviewPolicy.intervalSeconds</code>。
     */
    @Test void graduationConsolidatesAndFailureImmediatelyExits() {
        assertEquals(14, VocabularyHighIntensityReviewPolicy.nextStep(13, "KNOW", true));
        assertEquals(14, VocabularyHighIntensityReviewPolicy.nextStep(14, "KNOW", true));
        assertEquals(35L * 86400, VocabularyHighIntensityReviewPolicy.intervalSeconds(14));
        assertEquals(2, VocabularyHighIntensityReviewPolicy.nextStep(14, "UNCERTAIN", false));
        assertEquals(0, VocabularyHighIntensityReviewPolicy.nextStep(14, "FORGOT", false));
        assertEquals(13, VocabularyHighIntensityReviewPolicy.nextStep(14, "KNOW", false));
    }
    /**
     * 站点日UsesShanghaiMidnight及Leap日处理入口。
     * 关键处理步骤调用 <code>VocabularyMasteryCalculator.siteDay</code>、<code>LocalDateTime.of</code>、<code>VocabularyMasteryCalculator.graduated</code>、<code>goodModes</code>。
     */
    @Test void siteDayUsesShanghaiMidnightAndLeapDay() {
        assertEquals("2024-02-29", VocabularyMasteryCalculator.siteDay(LocalDateTime.of(2024, 2, 28, 16, 0)).toString());
        assertEquals("2026-10-09", VocabularyMasteryCalculator.siteDay(LocalDateTime.of(2026, 10, 8, 16, 0)).toString());
        var beforeMidnight = LocalDateTime.of(2026, 10, 1, 15, 59);
        assertTrue(VocabularyMasteryCalculator.graduated(goodModes(3), beforeMidnight, beforeMidnight.plusDays(7), 3, 2));
        assertFalse(VocabularyMasteryCalculator.graduated(goodModes(3), beforeMidnight, beforeMidnight.plusDays(6), 3, 2));
    }
    /**
     * early触发时间及OverdueDo不SkipStages处理入口。
     * 主要协作调用：<code>first.plusMinutes</code>、<code>first.plusDays</code>。
     */
    @Test void earlyOnTimeAndOverdueDoNotSkipStages() {
        assertEquals("NEW", VocabularyHighIntensityReviewPolicy.timing(null, first));
        assertEquals("EARLY", VocabularyHighIntensityReviewPolicy.timing(first.plusMinutes(1), first));
        assertEquals("ON_TIME", VocabularyHighIntensityReviewPolicy.timing(first, first.plusMinutes(5)));
        assertEquals("OVERDUE", VocabularyHighIntensityReviewPolicy.timing(first, first.plusDays(3)));
        assertEquals(1, VocabularyHighIntensityReviewPolicy.nextStep(0, "KNOW", false));
    }
}
