package com.starrainnotes.english.api;

import com.starrainnotes.english.api.dto.EnglishSearchDocument;
import java.util.List;

/* Public content only; private writing and uncleared reading never cross this boundary. */
public interface EnglishSearchSourceApi {
    List<EnglishSearchDocument> page(String contentType, Long afterId, int limit);

    EnglishSearchDocument findPublished(String contentType, Long contentId);
}
