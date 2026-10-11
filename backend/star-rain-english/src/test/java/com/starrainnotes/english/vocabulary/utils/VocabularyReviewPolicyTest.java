/*
 * VocabularyReviewPolicyTest：英语学习模块的测试文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：VocabularyReviewSchedule、TimingStatus。
 * 阅读入口：intervalTableMatchesLegacyTenStages（间隔TableMatchesLegacyTenStages处理入口）；intervalBeyondTenthReviewStaysAtLastStage（间隔BeyondTenth审核Stays时间最近Stage处理入口）；firstReviewIsNewAndUsesFirstInterval（首次审核是否New及Uses首次间隔处理入口）；timingStatusFollowsLegacyGraceWindow（调度状态FollowsLegacyGrace时间窗口处理入口）。
 */

package com.starrainnotes.english.vocabulary.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.starrainnotes.english.vocabulary.dto.VocabularyReviewSchedule;
import com.starrainnotes.english.vocabulary.enumeration.TimingStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/*
 * 固定间隔表与时机判定是 V1 的历史口径，改动会让已有 interval_seconds / timing_status 语义漂移，
 * 因此逐值锁在测试里。
 */
/**
 * 词汇审核策略测试，属于英语学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class VocabularyReviewPolicyTest {

    /**
     * 间隔TableMatchesLegacyTenStages处理入口。
     * 关键处理步骤调用 <code>VocabularyReviewPolicy.intervalSeconds</code>。
     */
    @Test
    void intervalTableMatchesLegacyTenStages() {
        long[] expected = {300L, 1_800L, 43_200L, 86_400L, 172_800L,
                345_600L, 604_800L, 1_296_000L, 2_592_000L, 5_184_000L};
        for (int index = 0; index < expected.length; index++) {
            assertThat(VocabularyReviewPolicy.intervalSeconds(index + 1)).isEqualTo(expected[index]);
        }
    }

    /**
     * 间隔BeyondTenth审核Stays时间最近Stage处理入口。
     * 关键处理步骤调用 <code>VocabularyReviewPolicy.intervalSeconds</code>。
     */
    @Test
    void intervalBeyondTenthReviewStaysAtLastStage() {
        assertThat(VocabularyReviewPolicy.intervalSeconds(11)).isEqualTo(5_184_000L);
        assertThat(VocabularyReviewPolicy.intervalSeconds(99)).isEqualTo(5_184_000L);
        assertThat(VocabularyReviewPolicy.intervalSeconds(0)).isEqualTo(300L);
    }

    /**
     * 首次审核是否New及Uses首次间隔处理入口。
     * 关键处理步骤调用 <code>LocalDateTime.of</code>、<code>VocabularyReviewPolicy.next</code>、<code>schedule.getReviewNumber</code>、<code>schedule.getReviewStep</code>。
     */
    @Test
    void firstReviewIsNewAndUsesFirstInterval() {
        LocalDateTime reviewedAt = LocalDateTime.of(2026, 3, 1, 2, 0);

        VocabularyReviewSchedule schedule = VocabularyReviewPolicy.next(0, null, reviewedAt);

        assertThat(schedule.getReviewNumber()).isEqualTo(1);
        assertThat(schedule.getReviewStep()).isEqualTo(1);
        assertThat(schedule.getIntervalSeconds()).isEqualTo(300L);
        assertThat(schedule.getTimingStatus()).isEqualTo(TimingStatus.NEW.name());
        assertThat(schedule.getNextReviewAt()).isEqualTo(reviewedAt.plusSeconds(300L));
    }

    /**
     * 调度状态FollowsLegacyGrace时间窗口处理入口。
     * 关键处理步骤调用 <code>LocalDateTime.of</code>、<code>VocabularyReviewPolicy.timingStatus</code>、<code>scheduled.minusMinutes</code>、<code>TimingStatus.EARLY.name</code>。
     */
    @Test
    void timingStatusFollowsLegacyGraceWindow() {
        LocalDateTime scheduled = LocalDateTime.of(2026, 3, 1, 2, 0);

        assertThat(VocabularyReviewPolicy.timingStatus(scheduled, scheduled.minusMinutes(1)))
                .isEqualTo(TimingStatus.EARLY.name());
        assertThat(VocabularyReviewPolicy.timingStatus(scheduled, scheduled))
                .isEqualTo(TimingStatus.ON_TIME.name());
        assertThat(VocabularyReviewPolicy.timingStatus(scheduled, scheduled.plusHours(23)))
                .isEqualTo(TimingStatus.ON_TIME.name());
        assertThat(VocabularyReviewPolicy.timingStatus(scheduled, scheduled.plusHours(25)))
                .isEqualTo(TimingStatus.OVERDUE.name());
    }

    /**
     * 审核StepStops时间TenWhile审核数值保留Growing处理入口。
     * 关键处理步骤调用 <code>VocabularyReviewPolicy.next</code>、<code>LocalDateTime.of</code>、<code>schedule.getReviewNumber</code>、<code>schedule.getReviewStep</code>。
     */
    @Test
    void reviewStepStopsAtTenWhileReviewNumberKeepsGrowing() {
        VocabularyReviewSchedule schedule = VocabularyReviewPolicy.next(
                25, LocalDateTime.of(2026, 3, 1, 2, 0), LocalDateTime.of(2026, 3, 2, 2, 0));

        assertThat(schedule.getReviewNumber()).isEqualTo(26);
        assertThat(schedule.getReviewStep()).isEqualTo(10);
        assertThat(schedule.getIntervalSeconds()).isEqualTo(5_184_000L);
    }
}
