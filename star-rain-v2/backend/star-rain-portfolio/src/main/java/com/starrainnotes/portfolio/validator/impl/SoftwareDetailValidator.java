package com.starrainnotes.portfolio.validator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.portfolio.enumeration.WorkType;
import com.starrainnotes.portfolio.exception.WorkInvalidException;
import com.starrainnotes.portfolio.validator.WorkDetailValidator;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SoftwareDetailValidator extends AbstractWorkDetailValidator implements WorkDetailValidator {
    @Override
    public WorkType supports() {
        return WorkType.SOFTWARE;
    }

    @Override
    public void validate(JsonNode detail) {
        fields(detail, Set.of("techStack", "role", "projectStage"));
        JsonNode stack = detail.get("techStack");
        if (stack == null || !stack.isArray() || stack.isEmpty() || stack.size() > 20) {
            throw new WorkInvalidException("techStack 必须包含 1～20 项");
        }
        for (JsonNode item : stack) {
            if (!item.isTextual() || item.asText().isBlank() || item.asText().length() > 60) {
                throw new WorkInvalidException("技术栈名称无效");
            }
        }
        text(detail, "role");
        if (!Set.of("DEVELOPING", "COMPLETED", "ONLINE").contains(text(detail, "projectStage"))) {
            throw new WorkInvalidException("项目阶段无效");
        }
    }
}
