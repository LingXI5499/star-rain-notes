/*
 * LearningProgressService：教程与学习模块的业务契约文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：LearningProgressDTO、LearningProgressVO、LearningTutorialProgressVO。
 * 阅读入口：save（保存学习进度）；complete（完成学习进度）；chapter（章节处理入口）；tutorial（教程处理入口）。
 */

package com.starrainnotes.tutorial.learning.service;

import com.starrainnotes.tutorial.learning.dto.LearningProgressDTO;
import com.starrainnotes.tutorial.learning.vo.LearningProgressVO;
import com.starrainnotes.tutorial.learning.vo.LearningTutorialProgressVO;
import java.util.List;

/**
 * 学习进度业务服务，属于教程与学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface LearningProgressService {
    /**
     * 保存学习进度。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>LearningProgressDTO</code>
     * @return 类型为 <code>LearningProgressVO</code> 的处理结果
     */
    LearningProgressVO save(Long chapterId, LearningProgressDTO request);
    /**
     * 完成学习进度。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>LearningProgressVO</code> 的处理结果
     */
    LearningProgressVO complete(Long chapterId);
    /**
     * 章节处理入口。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>LearningProgressVO</code> 的处理结果
     */
    LearningProgressVO chapter(Long chapterId);
    /**
     * 教程处理入口。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 类型为 <code>LearningTutorialProgressVO</code> 的处理结果
     */
    LearningTutorialProgressVO tutorial(Long tutorialId);
    /**
     * recent处理入口。
     * @return 本次处理得到的条目集合
     */
    List<LearningProgressVO> recent();
}
