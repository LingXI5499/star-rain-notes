package com.starrainnotes.english.listening.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.review.api.ReviewedContentHandler;
import com.starrainnotes.english.listening.dto.PronunciationRuleRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PronunciationRuleReviewHandler implements ReviewedContentHandler {
    private final PronunciationRuleCommandService commands;
    private final ObjectMapper json;

    public PronunciationRuleReviewHandler(PronunciationRuleCommandService commands, ObjectMapper json) {
        this.commands = commands;
        this.json = json;
    }

    @Override
    public boolean supports(String contentType, String actionType) {
        return "ENGLISH_PRONUNCIATION_RULE".equals(contentType) && "UPDATE".equals(actionType);
    }

    @Override
    public void apply(long contentId, Map<String, Object> payload) {
        commands.update(contentId, json.convertValue(payload, PronunciationRuleRequest.class));
    }
}
