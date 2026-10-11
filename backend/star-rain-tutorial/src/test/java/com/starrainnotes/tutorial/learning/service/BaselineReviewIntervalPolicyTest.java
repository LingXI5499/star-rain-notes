/*
 * BaselineReviewIntervalPolicyTest：教程与学习模块的测试文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：RecallRating、LearningReviewProperties、BaselineReviewIntervalPolicy、ReviewIntervalDecision。
 * 阅读入口：policy（策略处理入口）；advancesByRecallQualityAndCapsAtLastStep（按RecallQuality及Caps时间最近Step读取advances）；forgottenCardRetreatsAndHardCardGetsShorterGrowth（forgotten卡片Retreats及Hard卡片GetsShorterGrowth处理入口）；invalidScheduleConfigurationFailsClosed（无效ScheduleConfigurationFailsClosed处理入口）。
 */

package com.starrainnotes.tutorial.learning.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.starrainnotes.tutorial.learning.enumeration.RecallRating;
import com.starrainnotes.tutorial.learning.properties.LearningReviewProperties;
import com.starrainnotes.tutorial.learning.service.impl.BaselineReviewIntervalPolicy;
import com.starrainnotes.tutorial.learning.vo.ReviewIntervalDecision;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Baseline审核间隔策略测试，属于教程与学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class BaselineReviewIntervalPolicyTest {
    /**
     * 策略处理入口。
     * 关键处理步骤调用 <code>properties.setBaseIntervalDays</code>。
     *
     * @param intervals intervals；类型为 <code>List&lt;Integer&gt;</code>
     * @return 类型为 <code>BaselineReviewIntervalPolicy</code> 的处理结果
     */
    private BaselineReviewIntervalPolicy policy(List<Integer> intervals) {
        LearningReviewProperties properties = new LearningReviewProperties();
        properties.setBaseIntervalDays(intervals);
        return new BaselineReviewIntervalPolicy(properties);
    }

    /**
     * 按RecallQuality及Caps时间最近Step读取advances。
     * 关键处理步骤调用 <code>policy</code>、<code>List.of</code>、<code>policy.next</code>。
     */
    @Test
    void advancesByRecallQualityAndCapsAtLastStep() {
        BaselineReviewIntervalPolicy policy = policy(List.of(1, 3, 7, 14, 30, 60));
        assertDecision(policy.next(1, 3, RecallRating.NORMAL), 2, 7);
        assertDecision(policy.next(1, 3, RecallRating.EASY), 3, 14);
        assertDecision(policy.next(5, 60, RecallRating.EASY), 5, 60);
    }

    /**
     * forgotten卡片Retreats及Hard卡片GetsShorterGrowth处理入口。
     * 关键处理步骤调用 <code>policy</code>、<code>List.of</code>、<code>policy.next</code>。
     */
    @Test
    void forgottenCardRetreatsAndHardCardGetsShorterGrowth() {
        BaselineReviewIntervalPolicy policy = policy(List.of(1, 3, 7, 14, 30, 60));
        assertDecision(policy.next(4, 30, RecallRating.FORGOT), 2, 7);
        assertDecision(policy.next(0, 1, RecallRating.FORGOT), 0, 1);
        assertDecision(policy.next(2, 7, RecallRating.HARD), 2, 11);
    }

    /**
     * 无效ScheduleConfigurationFailsClosed处理入口。
     * 关键处理步骤调用 <code>policy</code>、<code>List.of</code>。
     */
    @Test
    void invalidScheduleConfigurationFailsClosed() {
        assertThrows(IllegalStateException.class,
                () -> policy(List.of()).next(0, 1, RecallRating.NORMAL));
        assertThrows(IllegalStateException.class,
                () -> policy(List.of(0, 3)).next(0, 1, RecallRating.NORMAL));
    }

    /**
     * 校验决定。
     * 关键处理步骤调用 <code>decision.getStepIndex</code>、<code>decision.getIntervalDays</code>。
     *
     * @param decision 决定；类型为 <code>ReviewIntervalDecision</code>
     * @param step step；类型为 <code>int</code>
     * @param days days；类型为 <code>int</code>
     */
    private void assertDecision(ReviewIntervalDecision decision, int step, int days) {
        assertEquals(step, decision.getStepIndex());
        assertEquals(days, decision.getIntervalDays());
    }
}
