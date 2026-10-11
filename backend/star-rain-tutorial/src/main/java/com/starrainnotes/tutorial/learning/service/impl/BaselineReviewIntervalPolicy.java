/*
 * BaselineReviewIntervalPolicy：教程与学习模块的业务实现文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：RecallRating、LearningReviewProperties、ReviewIntervalPolicy、ReviewIntervalDecision。
 * 阅读入口：next（下一项处理入口）。
 */

package com.starrainnotes.tutorial.learning.service.impl;

import com.starrainnotes.tutorial.learning.enumeration.RecallRating;
import com.starrainnotes.tutorial.learning.properties.LearningReviewProperties;
import com.starrainnotes.tutorial.learning.service.ReviewIntervalPolicy;
import com.starrainnotes.tutorial.learning.vo.ReviewIntervalDecision;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Baseline审核间隔策略，属于教程与学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Component
@RequiredArgsConstructor
public class BaselineReviewIntervalPolicy implements ReviewIntervalPolicy {
    /**
     * 本模块的外部配置属性。
     * 协作类型：<code>LearningReviewProperties</code>。
     */
    private final LearningReviewProperties properties;

    /**
     * 下一项处理入口。
     * 主要协作调用：<code>properties.getBaseIntervalDays</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>、<code>IllegalArgumentException</code>；具体触发条件见对应分支。
     *
     * @param stepIndex step索引；类型为 <code>int</code>
     * @param currentIntervalDays 当前间隔Days；类型为 <code>int</code>
     * @param rating 评分；类型为 <code>RecallRating</code>
     * @return 类型为 <code>ReviewIntervalDecision</code> 的处理结果
     */
    @Override
    public ReviewIntervalDecision next(int stepIndex, int currentIntervalDays, RecallRating rating) {
        List<Integer> intervals = properties.getBaseIntervalDays();
        if (intervals == null || intervals.isEmpty() || intervals.stream().anyMatch(day -> day == null || day < 1)) {
            throw new IllegalStateException("复习间隔配置必须是正整数序列");
        }
        int last = intervals.size() - 1;
        int current = Math.max(0, Math.min(stepIndex, last));
        int nextStep;
        int nextDays;
        switch (rating) {
            case FORGOT -> {
                nextStep = Math.max(0, current - 2);
                nextDays = intervals.get(nextStep);
            }
            case HARD -> {
                nextStep = current;
                nextDays = Math.max(intervals.get(0), Math.min(intervals.get(last),
                        Math.max(currentIntervalDays + 1, (int) Math.ceil(currentIntervalDays * 1.5))));
            }
            case NORMAL -> {
                nextStep = Math.min(last, current + 1);
                nextDays = intervals.get(nextStep);
            }
            case EASY -> {
                nextStep = Math.min(last, current + 2);
                nextDays = intervals.get(nextStep);
            }
            default -> throw new IllegalArgumentException("未知回忆评价");
        }
        return ReviewIntervalDecision.builder().stepIndex(nextStep).intervalDays(nextDays).build();
    }
}
