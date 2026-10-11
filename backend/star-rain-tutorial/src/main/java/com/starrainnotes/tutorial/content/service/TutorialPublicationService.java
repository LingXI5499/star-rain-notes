/*
 * TutorialPublicationService：教程与学习模块的业务契约文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：PageResult、TutorialAdminVO。
 * 阅读入口：refreshPublished（刷新已发布）；publishChapter（发布章节）；withdrawChapter（撤回章节）；submitReview（提交审核）。
 */

package com.starrainnotes.tutorial.content.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.vo.TutorialAdminVO;
import java.util.List;

/**
 * 教程发布业务服务，属于教程与学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface TutorialPublicationService {
    /**
     * 刷新已发布。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @param operation 操作；类型为 <code>String</code>
     * @param changedChapterId changed章节标识；类型为 <code>Long</code>
     */
    void refreshPublished(Long tutorialId, String operation, Long changedChapterId);
    /**
     * 发布章节。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     */
    void publishChapter(Long chapterId);
    /**
     * 撤回章节。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     */
    void withdrawChapter(Long chapterId);
    /**
     * 提交审核。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>
     */
    void submitReview(Long tutorialId, Long chapterId);
    /**
     * 发布教程发布。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialAdminVO</code> 的处理结果
     */
    TutorialAdminVO publish(Long tutorialId);
    /**
     * 撤回教程发布。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialAdminVO</code> 的处理结果
     */
    TutorialAdminVO withdraw(Long tutorialId);
    /**
     * 预览教程。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    JsonNode previewTutorial(Long tutorialId);
    /**
     * 预览章节。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    JsonNode previewChapter(Long chapterId);
    /**
     * 公开Categories处理入口。
     * @return 本次处理得到的条目集合
     */
    List<JsonNode> publicCategories();
    /**
     * 公开Tutorials处理入口。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>
     * @param search 搜索；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;JsonNode&gt;</code> 的处理结果
     */
    PageResult<JsonNode> publicTutorials(Long categoryId, String search, int page, int pageSize);
    /**
     * 公开教程处理入口。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    JsonNode publicTutorial(String slug);
    /**
     * 公开章节处理入口。
     *
     * @param tutorialSlug 教程固定链接标识；类型为 <code>String</code>
     * @param chapterSlug 章节固定链接标识；类型为 <code>String</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    JsonNode publicChapter(String tutorialSlug, String chapterSlug);
}
