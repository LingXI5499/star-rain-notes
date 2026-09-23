package com.starrainnotes.english.shared.content;


public interface EnglishContentDescriptorProvider {
    EnglishContentType type();
    ContentDescriptor require(long id);
    default ContentCatalogSlice catalog(ContentCatalogFilter filter, int limit) {
        throw new UnsupportedOperationException("Catalog is unavailable for " + type());
    }
    long publishedCount();
}
