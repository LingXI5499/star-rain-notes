package com.starrainnotes.portfolio.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.portfolio.enumeration.WorkType;

public interface WorkDetailValidator {
    WorkType supports();

    void validate(JsonNode detail);
}
