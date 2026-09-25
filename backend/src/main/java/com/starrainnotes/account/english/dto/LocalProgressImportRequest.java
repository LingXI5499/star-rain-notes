package com.starrainnotes.account.english.dto;

import java.util.Map;

public record LocalProgressImportRequest(Map<String, Object> vocabulary, Map<String, Object> learningRecords) {
}
