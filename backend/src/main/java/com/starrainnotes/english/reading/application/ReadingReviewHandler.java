package com.starrainnotes.english.reading.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.review.api.ReviewedContentHandler;
import com.starrainnotes.english.reading.dto.ReadingArticleRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReadingReviewHandler implements ReviewedContentHandler {
    private final ReadingCommandService commands;
    private final ObjectMapper json;

    public ReadingReviewHandler(ReadingCommandService commands, ObjectMapper json) {
        this.commands = commands;
        this.json = json;
    }

    @Override
    public boolean supports(String contentType, String actionType) {
        return "ENGLISH_READING_ARTICLE".equals(contentType) && "UPDATE".equals(actionType);
    }

    @Override
    public void apply(long contentId, Map<String, Object> payload) {
        commands.update(contentId, json.convertValue(payload, ReadingArticleRequest.class));
    }
}
