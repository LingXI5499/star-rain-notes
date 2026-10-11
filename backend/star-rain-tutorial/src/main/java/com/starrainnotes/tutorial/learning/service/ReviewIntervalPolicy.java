/*
 * ReviewIntervalPolicy：教程与学习模块的业务契约文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：RecallRating、ReviewIntervalDecision。
 * 阅读入口：next（下一项处理入口）。
 */

package com.starrainnotes.tutorial.learning.service;

import com.starrainnotes.tutorial.learning.enumeration.RecallRating;
import com.starrainnotes.tutorial.learning.vo.ReviewIntervalDecision;

/**
 * 审核间隔策略，属于教程与学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface ReviewIntervalPolicy {
    /**
     * 下一项处理入口。
     *
     * @param stepIndex step索引；类型为 <code>int</code>
     * @param currentIntervalDays 当前间隔Days；类型为 <code>int</code>
     * @param rating 评分；类型为 <code>RecallRating</code>
     * @return 类型为 <code>ReviewIntervalDecision</code> 的处理结果
     */
    ReviewIntervalDecision next(int stepIndex, int currentIntervalDays, RecallRating rating);
}
