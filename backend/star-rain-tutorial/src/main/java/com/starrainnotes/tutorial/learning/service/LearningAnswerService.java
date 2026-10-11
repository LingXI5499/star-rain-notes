/*
 * LearningAnswerService：教程与学习模块的业务契约文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：LearningAnswerDTO、LearningAnswerVO。
 * 阅读入口：answer（答案处理入口）；ownAnswer（own答案处理入口）；referenceAnswer（引用答案处理入口）。
 */

package com.starrainnotes.tutorial.learning.service;

import com.starrainnotes.tutorial.learning.dto.LearningAnswerDTO;
import com.starrainnotes.tutorial.learning.vo.LearningAnswerVO;

/**
 * 学习答案业务服务，属于教程与学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface LearningAnswerService {
    /**
     * 答案处理入口。
     *
     * @param questionId 问题标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>LearningAnswerDTO</code>
     * @return 类型为 <code>LearningAnswerVO</code> 的处理结果
     */
    LearningAnswerVO answer(Long questionId, LearningAnswerDTO request);
    /**
     * own答案处理入口。
     *
     * @param questionId 问题标识；类型为 <code>Long</code>
     * @return 类型为 <code>LearningAnswerVO</code> 的处理结果
     */
    LearningAnswerVO ownAnswer(Long questionId);
    /**
     * 引用答案处理入口。
     *
     * @param questionId 问题标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    String referenceAnswer(Long questionId);
}
