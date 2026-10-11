package com.starrainnotes.portfolio.validator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.portfolio.enumeration.WorkType;
import com.starrainnotes.portfolio.validator.WorkDetailValidator;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class WritingDetailValidator extends AbstractWorkDetailValidator implements WorkDetailValidator {
    @Override
    public WorkType supports() {
        return WorkType.WRITING;
    }

    @Override
    public void validate(JsonNode detail) {
        fields(detail, Set.of("publication", "wordCount"));
        text(detail, "publication");
        positiveInt(detail, "wordCount", 10_000_000);
    }
}
