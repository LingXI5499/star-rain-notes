package com.starrainnotes.english.writing.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.writing.dto.WritingPromptRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/** Business rules for writing prompt input and publication. */
@Component
public class WritingPromptPolicy {
    private final ObjectMapper json;

    public WritingPromptPolicy(ObjectMapper json) { this.json = json; }

    public void validateRequest(WritingPromptRequest request) {
        if (request.wordMin() < 0 || request.wordMax() < request.wordMin() || request.estimatedMinutes() < 0)
            invalid("Invalid word range or estimated time");
        validateRubric(request.rubricJson());
        validateChecklist(request.checklistJson());
    }

    public void requireTagDimension(String dimension) {
        if (dimension == null || !(dimension.equals("TOPIC") || dimension.equals("GENRE")
                || dimension.equals("FUNCTION") || dimension.equals("ABILITY"))) {
            invalid("Invalid writing tag");
        }
    }

    public void validatePublication(String background, String requirements, boolean hasTopicOrGenre,
                                    boolean templatePublished, boolean modelPublished,
                                    Long templateId, Long modelId) {
        if (background == null || background.isBlank()) publishInvalid("背景不能为空");
        if (requirements == null || requirements.isBlank()) publishInvalid("要求不能为空");
        if (!hasTopicOrGenre) publishInvalid("至少需要一个主题或文体标签");
        if (templateId != null && !templatePublished) publishInvalid("模板必须已发布");
        if (modelId != null && !modelPublished) publishInvalid("范文必须已发布");
    }

    private void validateRubric(String raw) {
        if (raw == null || raw.isBlank()) return;
        try {
            JsonNode node = json.readTree(raw);
            if (!node.isArray()) invalid("Rubric must be an array");
            Set<String> names = new HashSet<>(); int sum = 0;
            for (JsonNode item : node) {
                String name = item.path("name").asText(); int score = item.path("maxScore").asInt(0);
                if (name.isBlank() || !names.add(name) || score <= 0) invalid("Invalid rubric item");
                sum += score;
            }
            if (sum > 100) invalid("Rubric total cannot exceed 100");
        } catch (ApiException ex) { throw ex; }
        catch (Exception ex) { invalid("Invalid rubric JSON"); }
    }

    private void validateChecklist(String raw) {
        if (raw == null || raw.isBlank()) return;
        try {
            JsonNode node = json.readTree(raw);
            if (!node.isArray()) invalid("Checklist must be an array");
            Set<String> values = new HashSet<>();
            for (JsonNode item : node) {
                String value = item.asText().trim();
                if (value.isBlank() || !values.add(value)) invalid("Invalid checklist item");
            }
        } catch (ApiException ex) { throw ex; }
        catch (Exception ex) { invalid("Invalid checklist JSON"); }
    }

    private void invalid(String detail) {
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_WRITING_INVALID", "Invalid writing data", detail);
    }

    private void publishInvalid(String detail) {
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_WRITING_PUBLISH_INVALID", "Cannot publish", detail);
    }
}
