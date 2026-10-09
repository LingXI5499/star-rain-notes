package com.starrainnotes.tutorial.content.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Deletes logical children in dependency order inside the caller's transaction. */
@Repository
@RequiredArgsConstructor
public class TutorialDependencyCleanup {
    private final JdbcTemplate jdbc;
    private final TutorialLearningCleanupMapper learningCleanup;

    public void tutorialLearning(Long tutorialId) {
        retirePlans("TUTORIAL", tutorialId);
    }

    public void chapterChildren(Long chapterId) {
        retirePlans("CHAPTER", chapterId);
        learningCleanup.deleteChapterRelations(chapterId);
        jdbc.update("DELETE FROM sr_tutorial_knowledge_card WHERE chapter_id = ?", chapterId);
        jdbc.update("DELETE FROM sr_tutorial_question WHERE chapter_id = ?", chapterId);
    }

    public void groupLearning(Long groupId) {
        retirePlans("GROUP", groupId);
    }

    private void retirePlans(String scope, Long id) {
        learningCleanup.cancelPlans(scope, id);
        learningCleanup.skipCancelledTasks(scope, id);
    }

    public java.util.List<Long> revisionIds(Long tutorialId) {
        return jdbc.queryForList("SELECT id FROM sr_tutorial_revision WHERE tutorial_id = ?", Long.class, tutorialId);
    }

    public void revisions(Long tutorialId) {
        jdbc.update("DELETE FROM sr_tutorial_revision WHERE tutorial_id = ?", tutorialId);
    }
}
