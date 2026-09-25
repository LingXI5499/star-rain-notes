package com.starrainnotes.blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.review.api.ReviewedContentHandler;
import com.starrainnotes.blog.dto.UpdatePostRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BlogReviewHandler implements ReviewedContentHandler {
    private final BlogCommandService commands;
    private final ObjectMapper json;

    public BlogReviewHandler(BlogCommandService commands, ObjectMapper json) {
        this.commands = commands;
        this.json = json;
    }

    @Override
    public boolean supports(String contentType, String actionType) {
        return "BLOG_POST".equals(contentType) && "UPDATE".equals(actionType);
    }

    @Override
    public void apply(long contentId, Map<String, Object> payload) {
        commands.update(contentId, json.convertValue(payload, UpdatePostRequest.class));
    }
}
