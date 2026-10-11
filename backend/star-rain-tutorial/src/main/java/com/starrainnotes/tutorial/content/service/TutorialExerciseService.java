/*
 * TutorialExerciseService：教程与学习模块的业务契约文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：ChapterQuestionDTO、KnowledgeCardDTO。
 * 阅读入口：cards（卡片集合处理入口）；createCard（创建卡片）；updateCard（更新卡片）；deleteCard（删除卡片）。
 */

package com.starrainnotes.tutorial.content.service;

import com.starrainnotes.tutorial.content.dto.ChapterQuestionDTO;
import com.starrainnotes.tutorial.content.dto.KnowledgeCardDTO;
import java.util.List;
import java.util.Map;

/**
 * 教程Exercise业务服务，属于教程与学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface TutorialExerciseService {
    /**
     * 卡片集合处理入口。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    List<Map<String, Object>> cards(Long chapterId);
    /**
     * 创建卡片。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>KnowledgeCardDTO</code>
     * @return 类型为 <code>Map&lt;String, Object&gt;</code> 的处理结果
     */
    Map<String, Object> createCard(Long chapterId, KnowledgeCardDTO request);
    /**
     * 更新卡片。
     *
     * @param cardId 卡片标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>KnowledgeCardDTO</code>
     * @return 类型为 <code>Map&lt;String, Object&gt;</code> 的处理结果
     */
    Map<String, Object> updateCard(Long cardId, KnowledgeCardDTO request);
    /**
     * 删除卡片。
     *
     * @param cardId 卡片标识；类型为 <code>Long</code>
     */
    void deleteCard(Long cardId);
    /**
     * 调整顺序卡片集合。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    void reorderCards(Long chapterId, List<Long> ids);
    /**
     * 问题集合处理入口。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    List<Map<String, Object>> questions(Long chapterId);
    /**
     * 创建问题。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ChapterQuestionDTO</code>
     * @return 类型为 <code>Map&lt;String, Object&gt;</code> 的处理结果
     */
    Map<String, Object> createQuestion(Long chapterId, ChapterQuestionDTO request);
    /**
     * 更新问题。
     *
     * @param questionId 问题标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ChapterQuestionDTO</code>
     * @return 类型为 <code>Map&lt;String, Object&gt;</code> 的处理结果
     */
    Map<String, Object> updateQuestion(Long questionId, ChapterQuestionDTO request);
    /**
     * 删除问题。
     *
     * @param questionId 问题标识；类型为 <code>Long</code>
     */
    void deleteQuestion(Long questionId);
    /**
     * 调整顺序问题集合。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    void reorderQuestions(Long chapterId, List<Long> ids);
}
