/*
 * LearningDashboardService：教程与学习模块的业务契约文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：PageResult、LearningHistoryVO、LearningStatisticsVO。
 * 阅读入口：history（历史处理入口）；statistics（statistics处理入口）。
 */

package com.starrainnotes.tutorial.learning.service;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.learning.vo.LearningHistoryVO;
import com.starrainnotes.tutorial.learning.vo.LearningStatisticsVO;

/**
 * 学习概览业务服务，属于教程与学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface LearningDashboardService {
    /**
     * 历史处理入口。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;LearningHistoryVO&gt;</code> 的处理结果
     */
    PageResult<LearningHistoryVO> history(int page, int pageSize);
    /**
     * statistics处理入口。
     * @return 类型为 <code>LearningStatisticsVO</code> 的处理结果
     */
    LearningStatisticsVO statistics();
}
