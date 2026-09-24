package com.starrainnotes.english.api;

import java.util.Collection;
import java.util.Map;

/** Media capabilities needed by English content, without media persistence details. */
public interface MediaPort {
    boolean isImage(Long id);
    boolean isType(Long id, String type);
    String assetType(Long id);
    String publicUrl(Long id);
    Map<Long, String> publicUrls(Collection<Long> ids);
    void requireImageIfPresent(Long id);
}
