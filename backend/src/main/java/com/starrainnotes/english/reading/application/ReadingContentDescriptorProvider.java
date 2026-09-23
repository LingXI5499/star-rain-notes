package com.starrainnotes.english.reading.application;

import com.starrainnotes.english.reading.dto.ReadingArticleView;
import com.starrainnotes.english.shared.content.ContentDescriptor;
import com.starrainnotes.english.shared.content.ContentCatalogFilter;
import com.starrainnotes.english.shared.content.ContentCatalogSlice;
import com.starrainnotes.english.shared.content.EnglishContentDescriptorProvider;
import com.starrainnotes.english.shared.content.EnglishContentType;
import org.springframework.stereotype.Component;

@Component
public class ReadingContentDescriptorProvider implements EnglishContentDescriptorProvider {
    private final ReadingQueryService reading;
    public ReadingContentDescriptorProvider(ReadingQueryService reading) { this.reading=reading; }
    @Override public EnglishContentType type() { return EnglishContentType.READING; }
    @Override public ContentDescriptor require(long id) {
        ReadingArticleView item=reading.get(id);
        return new ContentDescriptor(type(),item.id(),item.slug(),item.title(),item.summary(),
                item.cefrLevel(),item.coverUrl(),item.publishStatus(),item.sortOrder()==null?0:item.sortOrder());
    }
    @Override public ContentCatalogSlice catalog(ContentCatalogFilter filter, int limit) {
        return reading.catalogDescriptors(filter, limit);
    }
    @Override public long publishedCount() {
        return catalog(new ContentCatalogFilter("PUBLISHED", null, null), 0).total();
    }
}
