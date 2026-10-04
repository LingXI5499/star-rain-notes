package com.starrainnotes.portfolio.validator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.portfolio.enumeration.WorkType;
import com.starrainnotes.portfolio.validator.WorkDetailValidator;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MusicDetailValidator extends AbstractWorkDetailValidator implements WorkDetailValidator {
    @Override
    public WorkType supports() {
        return WorkType.MUSIC;
    }

    @Override
    public void validate(JsonNode detail) {
        fields(detail, Set.of("artistRole", "durationSeconds"));
        text(detail, "artistRole");
        positiveInt(detail, "durationSeconds", 86400);
    }
}
