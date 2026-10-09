package com.starrainnotes.portfolio.validator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.portfolio.exception.WorkInvalidException;
import java.util.Iterator;
import java.util.Set;

abstract class AbstractWorkDetailValidator {
    void fields(JsonNode detail, Set<String> allowed) {
        if (detail == null || !detail.isObject()) {
            throw new WorkInvalidException("类型详情必须是对象");
        }
        Iterator<String> names = detail.fieldNames();
        while (names.hasNext()) {
            if (!allowed.contains(names.next())) {
                throw new WorkInvalidException("类型详情包含不支持的字段");
            }
        }
    }

    String text(JsonNode detail, String field) {
        JsonNode value = detail.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()
                || value.asText().length() > 200) {
            throw new WorkInvalidException(field + " 必须是 1～200 字的文本");
        }
        return value.asText().trim();
    }

    int positiveInt(JsonNode detail, String field, int max) {
        JsonNode value = detail.get(field);
        if (value == null || !value.isIntegralNumber() || value.asLong() < 1 || value.asLong() > max) {
            throw new WorkInvalidException(field + " 必须是有效正整数");
        }
        return value.asInt();
    }
}
