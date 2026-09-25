package com.starrainnotes.english.grammar.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.review.api.ReviewedContentHandler;
import com.starrainnotes.english.grammar.dto.GrammarLessonRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GrammarLessonReviewHandler implements ReviewedContentHandler {
    private final GrammarCommandService commands;
    private final ObjectMapper json;

    public GrammarLessonReviewHandler(GrammarCommandService commands, ObjectMapper json) {
        this.commands = commands;
        this.json = json;
    }

    @Override
    public boolean supports(String contentType, String actionType) {
        return "ENGLISH_GRAMMAR_LESSON".equals(contentType) && "UPDATE".equals(actionType);
    }

    @Override
    public void apply(long contentId, Map<String, Object> payload) {
        commands.updateLesson(contentId, json.convertValue(payload, GrammarLessonRequest.class));
    }
}
