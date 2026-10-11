/*
 * TutorialContentService：教程与学习模块的业务契约文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：PageResult、ChapterBodyDTO、ChapterCreateDTO、ChapterUpdateDTO、TutorialCreateDTO、TutorialUpdateDTO、TutorialAdminVO。
 * 阅读入口：categories（categories处理入口）；createCategory（创建分类）；updateCategory（更新分类）；deleteCategory（删除分类）。
 */

package com.starrainnotes.tutorial.content.service;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.dto.ChapterBodyDTO;
import com.starrainnotes.tutorial.content.dto.ChapterCreateDTO;
import com.starrainnotes.tutorial.content.dto.ChapterUpdateDTO;
import com.starrainnotes.tutorial.content.dto.TutorialCreateDTO;
import com.starrainnotes.tutorial.content.dto.TutorialUpdateDTO;
import com.starrainnotes.tutorial.content.vo.TutorialAdminVO;
import com.starrainnotes.tutorial.content.vo.TutorialCategoryVO;
import com.starrainnotes.tutorial.content.vo.TutorialChapterVO;
import com.starrainnotes.tutorial.content.vo.TutorialCurriculumVO;
import com.starrainnotes.tutorial.content.vo.TutorialGroupVO;
import java.util.List;

/**
 * 教程内容业务服务，属于教程与学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface TutorialContentService {
    /**
     * categories处理入口。
     * @return 本次处理得到的条目集合
     */
    List<TutorialCategoryVO> categories();
    /**
     * 创建分类。
     *
     * @param name 名称；类型为 <code>String</code>
     * @return 类型为 <code>TutorialCategoryVO</code> 的处理结果
     */
    TutorialCategoryVO createCategory(String name);
    /**
     * 更新分类。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param name 名称；类型为 <code>String</code>
     * @return 类型为 <code>TutorialCategoryVO</code> 的处理结果
     */
    TutorialCategoryVO updateCategory(Long id, String name);
    /**
     * 删除分类。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     */
    void deleteCategory(Long id);
    /**
     * 调整顺序Categories。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    void reorderCategories(List<Long> ids);

    /**
     * tutorials处理入口。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;TutorialAdminVO&gt;</code> 的处理结果
     */
    PageResult<TutorialAdminVO> tutorials(int page, int pageSize);
    /**
     * tutorials处理入口。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>
     * @param keyword 关键词；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;TutorialAdminVO&gt;</code> 的处理结果
     */
    PageResult<TutorialAdminVO> tutorials(Long categoryId, String keyword, int page, int pageSize);
    /**
     * 教程顺序处理入口。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>
     * @return 类型为 <code>java.util.List&lt;com.starrainnotes.tutorial.content.vo.TutorialOrderItemVO&gt;</code> 的处理结果
     */
    java.util.List<com.starrainnotes.tutorial.content.vo.TutorialOrderItemVO> tutorialOrder(Long categoryId);
    /**
     * 教程处理入口。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialAdminVO</code> 的处理结果
     */
    TutorialAdminVO tutorial(Long id);
    /**
     * 创建教程。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>TutorialCreateDTO</code>
     * @return 类型为 <code>TutorialAdminVO</code> 的处理结果
     */
    TutorialAdminVO createTutorial(TutorialCreateDTO request);
    /**
     * 更新教程。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>TutorialUpdateDTO</code>
     * @return 类型为 <code>TutorialAdminVO</code> 的处理结果
     */
    TutorialAdminVO updateTutorial(Long id, TutorialUpdateDTO request);
    /**
     * 删除教程。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     */
    void deleteTutorial(Long id);
    /**
     * 调整顺序Tutorials。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    void reorderTutorials(Long categoryId, List<Long> ids);

    /**
     * curriculum处理入口。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialCurriculumVO</code> 的处理结果
     */
    TutorialCurriculumVO curriculum(Long tutorialId);
    /**
     * 创建分组。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @param title 标题；类型为 <code>String</code>
     * @return 类型为 <code>TutorialGroupVO</code> 的处理结果
     */
    TutorialGroupVO createGroup(Long tutorialId, String title);
    /**
     * 更新分组。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>
     * @param title 标题；类型为 <code>String</code>
     * @return 类型为 <code>TutorialGroupVO</code> 的处理结果
     */
    TutorialGroupVO updateGroup(Long groupId, String title);
    /**
     * 删除分组。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>
     */
    void deleteGroup(Long groupId);
    /**
     * 调整顺序Groups。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    void reorderGroups(Long tutorialId, List<Long> ids);

    /**
     * 章节处理入口。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>TutorialChapterVO</code> 的处理结果
     */
    TutorialChapterVO chapter(Long chapterId);
    /**
     * 创建章节。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ChapterCreateDTO</code>
     * @return 类型为 <code>TutorialChapterVO</code> 的处理结果
     */
    TutorialChapterVO createChapter(Long groupId, ChapterCreateDTO request);
    /**
     * 更新章节。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ChapterUpdateDTO</code>
     * @return 类型为 <code>TutorialChapterVO</code> 的处理结果
     */
    TutorialChapterVO updateChapter(Long chapterId, ChapterUpdateDTO request);
    /**
     * 更新章节正文。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ChapterBodyDTO</code>
     * @return 类型为 <code>TutorialChapterVO</code> 的处理结果
     */
    TutorialChapterVO updateChapterBody(Long chapterId, ChapterBodyDTO request);
    /**
     * 删除章节。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     */
    void deleteChapter(Long chapterId);
    /**
     * 调整顺序章节列表。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    void reorderChapters(Long groupId, List<Long> ids);
}
