/*
 * TutorialDashboardService：教程与学习模块的业务契约文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：DashboardModuleData。
 * 阅读入口：summary（摘要处理入口）。
 */

package com.starrainnotes.tutorial.content.service;

import com.starrainnotes.common.dashboard.dto.DashboardModuleData;

/* 后台概览需要的教程模块摘要。 */
/**
 * 教程概览业务服务，属于教程与学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface TutorialDashboardService {
    /**
     * 摘要处理入口。
     * @return 类型为 <code>DashboardModuleData</code> 的处理结果
     */
    DashboardModuleData summary();
}
