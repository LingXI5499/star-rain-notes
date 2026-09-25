package com.starrainnotes.account.english.dto;

import java.util.List;
import java.util.Map;

public record LocalVocabularyImportRequest(Object memory, Object vocabulary, List<Map<String, Object>> reviewLog) {
}
