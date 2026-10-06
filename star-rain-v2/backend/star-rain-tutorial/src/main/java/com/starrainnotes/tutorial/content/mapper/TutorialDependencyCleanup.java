package com.starrainnotes.tutorial.content.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Deletes logical children in dependency order inside the caller's transaction. */
@Repository
@RequiredArgsConstructor
public class TutorialDependencyCleanup {
    private final JdbcTemplate jdbc;

    public void tutorialLearning(Long tutorialId) {
        jdbc.update("DELETE FROM sr_study_task WHERE tutorial_id = ?", tutorialId);
        jdbc.update("DELETE FROM sr_study_plan WHERE tutorial_id = ?", tutorialId);
        jdbc.update("DELETE FROM sr_learning_history WHERE tutorial_id = ?", tutorialId);
        jdbc.update("DELETE FROM sr_learning_progress WHERE tutorial_id = ?", tutorialId);
    }

    public void chapterChildren(Long chapterId) {
        String cardIds = "SELECT id FROM sr_tutorial_knowledge_card WHERE chapter_id = ?";
        String questionIds = "SELECT id FROM sr_tutorial_question WHERE chapter_id = ?";
        jdbc.update("DELETE FROM sr_learning_history WHERE chapter_id = ? OR knowledge_card_id IN (" + cardIds
                + ") OR study_task_id IN (SELECT id FROM sr_study_task WHERE chapter_id = ?)"
                + " OR review_task_id IN (SELECT id FROM sr_review_task WHERE knowledge_card_id IN (" + cardIds + "))",
                chapterId, chapterId, chapterId, chapterId);
        jdbc.update("DELETE FROM sr_study_task WHERE plan_id IN "
                + "(SELECT id FROM sr_study_plan WHERE scope_type = 'CHAPTER' AND scope_id = ?)"
                + " OR chapter_id = ? OR (target_type = 'CHAPTER' AND target_id = ?)",
                chapterId, chapterId, chapterId);
        jdbc.update("DELETE FROM sr_study_plan WHERE scope_type = 'CHAPTER' AND scope_id = ?", chapterId);
        jdbc.update("DELETE FROM sr_review_task WHERE knowledge_card_id IN (" + cardIds + ")", chapterId);
        jdbc.update("DELETE FROM sr_review_result WHERE knowledge_card_id IN (" + cardIds + ")", chapterId);
        jdbc.update("DELETE FROM sr_review_schedule WHERE knowledge_card_id IN (" + cardIds + ")", chapterId);
        jdbc.update("DELETE FROM sr_knowledge_mastery WHERE knowledge_card_id IN (" + cardIds + ")", chapterId);
        jdbc.update("DELETE FROM sr_user_question_answer WHERE question_id IN (" + questionIds + ")", chapterId);
        jdbc.update("DELETE FROM sr_learning_progress WHERE chapter_id = ?", chapterId);
        jdbc.update("DELETE FROM sr_tutorial_knowledge_card WHERE chapter_id = ?", chapterId);
        jdbc.update("DELETE FROM sr_tutorial_question WHERE chapter_id = ?", chapterId);
    }

    public void groupLearning(Long groupId) {
        jdbc.update("DELETE FROM sr_study_task WHERE plan_id IN "
                + "(SELECT id FROM sr_study_plan WHERE scope_type = 'GROUP' AND scope_id = ?)", groupId);
        jdbc.update("DELETE FROM sr_study_plan WHERE scope_type = 'GROUP' AND scope_id = ?", groupId);
        jdbc.update("DELETE FROM sr_learning_history WHERE group_id = ?", groupId);
        jdbc.update("DELETE FROM sr_learning_progress WHERE group_id = ?", groupId);
    }

    public java.util.List<Long> revisionIds(Long tutorialId) {
        return jdbc.queryForList("SELECT id FROM sr_tutorial_revision WHERE tutorial_id = ?", Long.class, tutorialId);
    }

    public void revisions(Long tutorialId) {
        jdbc.update("DELETE FROM sr_tutorial_revision WHERE tutorial_id = ?", tutorialId);
    }
}
