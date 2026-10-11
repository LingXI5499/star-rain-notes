/*
 * LearningReviewService：教程与学习模块的业务契约文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：RecallRating、MasteryVO、ReviewResultVO、ReviewTaskVO。
 * 阅读入口：initializeChapter（initialize章节处理入口）；today（today处理入口）；task（任务处理入口）；back（back处理入口）。
 */

package com.starrainnotes.tutorial.learning.service;

import com.starrainnotes.tutorial.learning.enumeration.RecallRating;
import com.starrainnotes.tutorial.learning.vo.MasteryVO;
import com.starrainnotes.tutorial.learning.vo.ReviewResultVO;
import com.starrainnotes.tutorial.learning.vo.ReviewTaskVO;
import java.util.List;

/**
 * 学习审核业务服务，属于教程与学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface LearningReviewService {
    /**
     * initialize章节处理入口。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>
     */
    void initializeChapter(Long accountId, Long chapterId);
    /**
     * today处理入口。
     * @return 本次处理得到的条目集合
     */
    List<ReviewTaskVO> today();
    /**
     * 任务处理入口。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>ReviewTaskVO</code> 的处理结果
     */
    ReviewTaskVO task(Long taskId);
    /**
     * back处理入口。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    String back(Long taskId);
    /**
     * 完成学习审核。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @param rating 评分；类型为 <code>RecallRating</code>
     * @return 类型为 <code>ReviewResultVO</code> 的处理结果
     */
    ReviewResultVO complete(Long taskId, RecallRating rating);
    /**
     * 掌握处理入口。
     * @return 本次处理得到的条目集合
     */
    List<MasteryVO> mastery();
    /**
     * self评分处理入口。
     *
     * @param cardId 卡片标识；类型为 <code>Long</code>
     * @param level level；类型为 <code>String</code>
     * @return 类型为 <code>MasteryVO</code> 的处理结果
     */
    MasteryVO selfRating(Long cardId, String level);
}
