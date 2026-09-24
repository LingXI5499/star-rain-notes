package com.starrainnotes.english.shared.content;

import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;
import java.util.List;

public interface EnglishContentDescriptorProvider {
    EnglishContentType type();
    ContentDescriptor require(long id);
    default ContentDescriptor requirePublished(long id) {
        ContentDescriptor descriptor = require(id);
        if (!descriptor.published()) throw new ApiException(HttpStatus.NOT_FOUND,
                "ENGLISH_CONTENT_NOT_PUBLISHED", "Content unavailable",
                "The selected English content is not published.");
        return descriptor;
    }
    default ContentCatalogSlice catalog(ContentCatalogFilter filter, int limit) {
        throw new UnsupportedOperationException("Catalog is unavailable for " + type());
    }
    long publishedCount();
    default List<Long> tagIds(long contentId) { return List.of(); }
    default List<TagMatchCandidate> tagMatches(List<Long> termIds) { return List.of(); }
    default List<ContentDescriptor> publishedCandidates() {
        return catalog(new ContentCatalogFilter("PUBLISHED", null, null), Integer.MAX_VALUE).items();
    }
}
