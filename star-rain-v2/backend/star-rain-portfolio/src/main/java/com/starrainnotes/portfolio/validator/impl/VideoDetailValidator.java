package com.starrainnotes.portfolio.validator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.portfolio.enumeration.WorkType;
import com.starrainnotes.portfolio.validator.WorkDetailValidator;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class VideoDetailValidator extends AbstractWorkDetailValidator implements WorkDetailValidator {
    @Override
    public WorkType supports() {
        return WorkType.VIDEO;
    }

    @Override
    public void validate(JsonNode detail) {
        fields(detail, Set.of("platform", "durationSeconds"));
        text(detail, "platform");
        positiveInt(detail, "durationSeconds", 86400);
    }
}
