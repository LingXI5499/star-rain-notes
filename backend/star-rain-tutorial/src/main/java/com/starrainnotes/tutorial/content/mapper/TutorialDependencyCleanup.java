/*
 * TutorialDependencyCleanup：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 阅读入口：tutorialLearning（教程学习处理入口）；chapterChildren（章节Children处理入口）；groupLearning（分组学习处理入口）；revisionIds（修订标识集合处理入口）。
 */

package com.starrainnotes.tutorial.content.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Deletes logical children in dependency order inside the caller's transaction. */
@Repository
@RequiredArgsConstructor
public class TutorialDependencyCleanup {
    /**
     * jdbc。
     */
    private final JdbcTemplate jdbc;
    /**
     * 学习清理。
     * 协作类型：<code>TutorialLearningCleanupMapper</code>。
     */
    private final TutorialLearningCleanupMapper learningCleanup;

    /**
     * 教程学习处理入口。
     * 关键处理步骤调用 <code>retirePlans</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     */
    public void tutorialLearning(Long tutorialId) {
        retirePlans("TUTORIAL", tutorialId);
    }

    /**
     * 章节Children处理入口。
     * 主要协作调用：<code>learningCleanup.deleteChapterRelations</code>、<code>jdbc.update</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     */
    public void chapterChildren(Long chapterId) {
        retirePlans("CHAPTER", chapterId);
        learningCleanup.deleteChapterRelations(chapterId);
        jdbc.update("DELETE FROM sr_tutorial_knowledge_card WHERE chapter_id = ?", chapterId);
        jdbc.update("DELETE FROM sr_tutorial_question WHERE chapter_id = ?", chapterId);
    }

    /**
     * 分组学习处理入口。
     * 关键处理步骤调用 <code>retirePlans</code>。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>
     */
    public void groupLearning(Long groupId) {
        retirePlans("GROUP", groupId);
    }

    /**
     * retirePlans处理入口。
     * 主要协作调用：<code>learningCleanup.cancelPlans</code>、<code>learningCleanup.skipCancelledTasks</code>。
     *
     * @param scope scope；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     */
    private void retirePlans(String scope, Long id) {
        learningCleanup.cancelPlans(scope, id);
        learningCleanup.skipCancelledTasks(scope, id);
    }

    /**
     * 修订标识集合处理入口。
     * 主要协作调用：<code>jdbc.queryForList</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 类型为 <code>java.util.List&lt;Long&gt;</code> 的处理结果
     */
    public java.util.List<Long> revisionIds(Long tutorialId) {
        return jdbc.queryForList("SELECT id FROM sr_tutorial_revision WHERE tutorial_id = ?", Long.class, tutorialId);
    }

    /**
     * 修订集合处理入口。
     * 主要协作调用：<code>jdbc.update</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     */
    public void revisions(Long tutorialId) {
        jdbc.update("DELETE FROM sr_tutorial_revision WHERE tutorial_id = ?", tutorialId);
    }
}
