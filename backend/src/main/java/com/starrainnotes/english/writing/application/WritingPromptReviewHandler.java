package com.starrainnotes.english.writing.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.review.api.ReviewedContentHandler;
import com.starrainnotes.english.writing.dto.WritingPromptRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WritingPromptReviewHandler implements ReviewedContentHandler {
    private final WritingPromptCommandService commands;
    private final ObjectMapper json;

    public WritingPromptReviewHandler(WritingPromptCommandService commands, ObjectMapper json) {
        this.commands = commands;
        this.json = json;
    }

    @Override
    public boolean supports(String contentType, String actionType) {
        return "ENGLISH_WRITING_PROMPT".equals(contentType) && "UPDATE".equals(actionType);
    }

    @Override
    public void apply(long contentId, Map<String, Object> payload) {
        commands.update(contentId, json.convertValue(payload, WritingPromptRequest.class));
    }
}
