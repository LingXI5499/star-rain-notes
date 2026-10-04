package com.starrainnotes.tutorial.content.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.vo.TutorialAdminVO;
import java.util.List;

public interface TutorialPublicationService {
    TutorialAdminVO publish(Long tutorialId);
    TutorialAdminVO withdraw(Long tutorialId);
    TutorialAdminVO restore(Long tutorialId);
    TutorialAdminVO submitReview(Long tutorialId);
    JsonNode previewTutorial(Long tutorialId);
    JsonNode previewChapter(Long chapterId);
    List<JsonNode> publicCategories();
    PageResult<JsonNode> publicTutorials(Long categoryId, String search, int page, int pageSize);
    JsonNode publicTutorial(String slug);
    JsonNode publicChapter(String tutorialSlug, String chapterSlug);
    JsonNode publicQuestionAnswer(String tutorialSlug, String chapterSlug, String questionId);
}
