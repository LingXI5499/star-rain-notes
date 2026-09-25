package com.starrainnotes.english.writing.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.writing.dto.WritingResourceRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/** Business rules for writing resource shape and publication readiness. */
@Component
public class WritingResourcePolicy {
    private static final Set<String> KINDS = Set.of("EXPRESSION_LESSON", "GENRE_LESSON", "MODEL_ESSAY", "TEMPLATE");
    private static final Set<String> LEVELS = Set.of("SENTENCE", "PARAGRAPH", "COHESION", "STYLE");
    private final ObjectMapper json;

    public WritingResourcePolicy(ObjectMapper json) { this.json = json; }

    public void validateRequest(WritingResourceRequest request) {
        if (!KINDS.contains(request.resourceKind())) invalid("Unknown resource kind");
        if (request.expressionLevel() != null && !request.expressionLevel().isBlank() && !LEVELS.contains(request.expressionLevel())) invalid("Unknown expression level");
        if ("EXPRESSION_LESSON".equals(request.resourceKind()) && (request.expressionLevel() == null || request.expressionLevel().isBlank())) invalid("Expression lessons require expressionLevel");
        if (!"EXPRESSION_LESSON".equals(request.resourceKind()) && request.expressionLevel() != null && !request.expressionLevel().isBlank()) invalid("expressionLevel is only allowed for expression lessons");
        if (!"TEMPLATE".equals(request.resourceKind()) && request.templateSchemaJson() != null && !request.templateSchemaJson().isBlank()) invalid("Only templates may contain templateSchemaJson");
        if ((request.wordMin() != null && request.wordMin() < 0) || (request.wordMax() != null && request.wordMax() < 0)
                || (request.wordMin() != null && request.wordMax() != null && request.wordMin() > request.wordMax())) invalid("Invalid word range");
        if (request.estimatedMinutes() != null && request.estimatedMinutes() < 0) invalid("Invalid time");
        if ("TEMPLATE".equals(request.resourceKind())) validateTemplate(request.templateSchemaJson());
    }

    public void requireTagDimension(String dimension) {
        if (dimension == null || !(dimension.equals("TOPIC") || dimension.equals("GENRE")
                || dimension.equals("FUNCTION") || dimension.equals("ABILITY"))) {
            invalid("Invalid writing tag");
        }
    }

    public void requireUnused(int promptReferences) {
        if (promptReferences > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "ENGLISH_WRITING_RESOURCE_IN_USE",
                    "Resource is in use", "Remove prompt references first.");
        }
    }

    public void validatePublication(String title, String body, boolean hasTopicOrGenre, String resourceKind, String schema) {
        if (title == null || title.isBlank()) publishInvalid("标题不能为空");
        if (body == null || body.isBlank()) publishInvalid("正文不能为空");
        if (!hasTopicOrGenre) publishInvalid("至少需要主题或文体标签");
        if ("TEMPLATE".equals(resourceKind) && (schema == null || schema.isBlank())) publishInvalid("模板必须有结构");
    }

    private void validateTemplate(String raw) {
        if (raw == null || raw.isBlank()) invalid("Templates require a schema");
        try {
            JsonNode root = json.readTree(raw);
            if (!root.isObject() || root.path("version").asInt() != 1 || !root.path("blocks").isArray()) invalid("Invalid template schema");
            Set<String> seen = new HashSet<>();
            for (JsonNode block : root.path("blocks")) {
                String id = block.path("id").asText(); String type = block.path("type").asText();
                if (!id.matches("[a-z0-9]+(?:-[a-z0-9]+)*") || !seen.add(id) || !(type.equals("text") || type.equals("textarea"))) invalid("Invalid template block");
            }
        } catch (ApiException ex) { throw ex; }
        catch (Exception ex) { invalid("Invalid template schema JSON"); }
    }

    private void invalid(String detail) {
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_WRITING_INVALID", "Invalid writing data", detail);
    }

    private void publishInvalid(String detail) {
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_WRITING_PUBLISH_INVALID", "Cannot publish", detail);
    }
}
