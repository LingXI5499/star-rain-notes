package com.starrainnotes.english.api.impl;

import com.starrainnotes.english.api.EnglishSearchSourceApi;
import com.starrainnotes.english.api.EnglishSearchTypes;
import com.starrainnotes.english.api.dto.EnglishSearchDocument;
import com.starrainnotes.english.knowledge.mapper.EnglishSearchSourceMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnglishSearchSourceApiAdapter implements EnglishSearchSourceApi {
    private final EnglishSearchSourceMapper mapper;

    @Override
    public List<EnglishSearchDocument> page(String contentType, Long afterId, int limit) {
        validateType(contentType);
        if (limit < 1 || limit > 100 || (afterId != null && afterId < 0)) {
            throw new IllegalArgumentException("Invalid English search page");
        }
        List<EnglishSearchDocument> documents = mapper.page(contentType, afterId, limit);
        documents.forEach(this::route);
        return documents;
    }

    @Override
    public EnglishSearchDocument findPublished(String contentType, Long contentId) {
        validateType(contentType);
        if (contentId == null || contentId <= 0) throw new IllegalArgumentException("Invalid English content id");
        EnglishSearchDocument document = mapper.findPublished(contentType, contentId);
        if (document != null) route(document);
        return document;
    }

    private void route(EnglishSearchDocument document) {
        if ("ENGLISH_VOCABULARY_WORD".equals(document.getContentType())) {
            String prefix = "/english/vocabulary/" + document.getThemeId() + "?q=";
            String query = document.getTitle();
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            // A long non-ASCII phrase may exceed the index URL limit after encoding.
            // A prefix query still includes the matching word on the theme page.
            while (prefix.length() + encoded.length() > 500) {
                query = query.substring(0, query.offsetByCodePoints(query.length(), -1));
                encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            }
            document.setRoutePath(prefix + encoded);
        }
    }

    private void validateType(String type) {
        if (!EnglishSearchTypes.ALL.contains(type)) throw new IllegalArgumentException("Invalid English search type");
    }
}
