package com.starrainnotes.portfolio.validator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.portfolio.enumeration.WorkType;
import com.starrainnotes.portfolio.validator.WorkDetailValidator;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class OtherDetailValidator extends AbstractWorkDetailValidator implements WorkDetailValidator {
    @Override
    public WorkType supports() {
        return WorkType.OTHER;
    }

    @Override
    public void validate(JsonNode detail) {
        fields(detail, Set.of("description"));
        if (detail.has("description")) {
            text(detail, "description");
        }
    }
}
