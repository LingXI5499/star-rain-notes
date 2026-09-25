package com.starrainnotes.tutorial.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.review.api.ReviewedContentHandler;
import com.starrainnotes.tutorial.dto.UpdateChapterRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TutorialChapterReviewHandler implements ReviewedContentHandler {
    private final TutorialChapterCommandService chapters;
    private final ObjectMapper json;

    public TutorialChapterReviewHandler(TutorialChapterCommandService chapters, ObjectMapper json) {
        this.chapters = chapters;
        this.json = json;
    }

    @Override
    public boolean supports(String contentType, String actionType) {
        return "TUTORIAL_CHAPTER".equals(contentType) && "UPDATE".equals(actionType);
    }

    @Override
    public void apply(long contentId, Map<String, Object> payload) {
        Long tutorialId = json.convertValue(payload.get("tutorialId"), Long.class);
        UpdateChapterRequest request = json.convertValue(payload.get("request"), UpdateChapterRequest.class);
        chapters.updateChapter(tutorialId, contentId, request);
    }
}
