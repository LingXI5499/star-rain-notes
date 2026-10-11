/*
 * StudyPlanService：教程与学习模块的业务契约文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：StudyPlanDTO、StudyPlanVO、StudyTaskVO。
 * 阅读入口：create（创建学习计划）；update（更新学习计划）；transition（transition处理入口）；plan（计划处理入口）。
 */

package com.starrainnotes.tutorial.learning.service;

import com.starrainnotes.tutorial.learning.dto.StudyPlanDTO;
import com.starrainnotes.tutorial.learning.vo.StudyPlanVO;
import com.starrainnotes.tutorial.learning.vo.StudyTaskVO;
import java.util.List;

/**
 * 学习计划业务服务，属于教程与学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface StudyPlanService {
    /**
     * 创建学习计划。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>StudyPlanDTO</code>
     * @return 类型为 <code>StudyPlanVO</code> 的处理结果
     */
    StudyPlanVO create(StudyPlanDTO request);
    /**
     * 更新学习计划。
     *
     * @param planId 计划标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>StudyPlanDTO</code>
     * @return 类型为 <code>StudyPlanVO</code> 的处理结果
     */
    StudyPlanVO update(Long planId, StudyPlanDTO request);
    /**
     * transition处理入口。
     *
     * @param planId 计划标识；类型为 <code>Long</code>
     * @param action 动作；类型为 <code>String</code>
     * @return 类型为 <code>StudyPlanVO</code> 的处理结果
     */
    StudyPlanVO transition(Long planId, String action);
    /**
     * 计划处理入口。
     *
     * @param planId 计划标识；类型为 <code>Long</code>
     * @return 类型为 <code>StudyPlanVO</code> 的处理结果
     */
    StudyPlanVO plan(Long planId);
    /**
     * plans处理入口。
     * @return 本次处理得到的条目集合
     */
    List<StudyPlanVO> plans();
    /**
     * 计划Tasks处理入口。
     *
     * @param planId 计划标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    List<StudyTaskVO> planTasks(Long planId);
    /**
     * today处理入口。
     * @return 本次处理得到的条目集合
     */
    List<StudyTaskVO> today();
    /**
     * 开始处理入口。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>StudyTaskVO</code> 的处理结果
     */
    StudyTaskVO start(Long taskId);
    /**
     * 完成学习计划。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>StudyTaskVO</code> 的处理结果
     */
    StudyTaskVO complete(Long taskId);
    /**
     * skip处理入口。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>StudyTaskVO</code> 的处理结果
     */
    StudyTaskVO skip(Long taskId);
}
