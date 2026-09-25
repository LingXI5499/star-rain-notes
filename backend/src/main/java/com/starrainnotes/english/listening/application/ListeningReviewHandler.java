package com.starrainnotes.english.listening.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.review.api.ReviewedContentHandler;
import com.starrainnotes.english.listening.dto.ListeningItemRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ListeningReviewHandler implements ReviewedContentHandler {
    private final ListeningCommandService commands;
    private final ObjectMapper json;

    public ListeningReviewHandler(ListeningCommandService commands, ObjectMapper json) {
        this.commands = commands;
        this.json = json;
    }

    @Override
    public boolean supports(String contentType, String actionType) {
        return "ENGLISH_LISTENING_ITEM".equals(contentType) && "UPDATE".equals(actionType);
    }

    @Override
    public void apply(long contentId, Map<String, Object> payload) {
        commands.update(contentId, json.convertValue(payload, ListeningItemRequest.class));
    }
}
