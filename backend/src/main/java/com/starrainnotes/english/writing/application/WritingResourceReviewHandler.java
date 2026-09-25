package com.starrainnotes.english.writing.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.review.api.ReviewedContentHandler;
import com.starrainnotes.english.writing.dto.WritingResourceRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WritingResourceReviewHandler implements ReviewedContentHandler {
    private final WritingResourceCommandService commands;
    private final ObjectMapper json;

    public WritingResourceReviewHandler(WritingResourceCommandService commands, ObjectMapper json) {
        this.commands = commands;
        this.json = json;
    }

    @Override
    public boolean supports(String contentType, String actionType) {
        return "ENGLISH_WRITING_RESOURCE".equals(contentType) && "UPDATE".equals(actionType);
    }

    @Override
    public void apply(long contentId, Map<String, Object> payload) {
        commands.update(contentId, json.convertValue(payload, WritingResourceRequest.class));
    }
}
