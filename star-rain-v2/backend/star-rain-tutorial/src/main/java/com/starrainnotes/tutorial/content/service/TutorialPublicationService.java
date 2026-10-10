package com.starrainnotes.tutorial.content.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.vo.TutorialAdminVO;
import java.util.List;

public interface TutorialPublicationService {
    void refreshPublished(Long tutorialId, String operation, Long changedChapterId);
    void publishChapter(Long chapterId);
    void withdrawChapter(Long chapterId);
    void submitReview(Long tutorialId, Long chapterId);
    TutorialAdminVO publish(Long tutorialId);
    TutorialAdminVO withdraw(Long tutorialId);
    JsonNode previewTutorial(Long tutorialId);
    JsonNode previewChapter(Long chapterId);
    List<JsonNode> publicCategories();
    PageResult<JsonNode> publicTutorials(Long categoryId, String search, int page, int pageSize);
    JsonNode publicTutorial(String slug);
    JsonNode publicChapter(String tutorialSlug, String chapterSlug);
}
