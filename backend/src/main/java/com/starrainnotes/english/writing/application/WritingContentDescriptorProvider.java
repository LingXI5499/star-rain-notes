package com.starrainnotes.english.writing.application;

import com.starrainnotes.english.shared.content.ContentDescriptor;
import com.starrainnotes.english.shared.content.ContentCatalogFilter;
import com.starrainnotes.english.shared.content.ContentCatalogSlice;
import com.starrainnotes.english.shared.content.EnglishContentDescriptorProvider;
import com.starrainnotes.english.shared.content.EnglishContentType;
import com.starrainnotes.english.writing.dto.WritingPromptView;
import org.springframework.stereotype.Component;

@Component
public class WritingContentDescriptorProvider implements EnglishContentDescriptorProvider {
    private final WritingPromptQueryService writing;
    public WritingContentDescriptorProvider(WritingPromptQueryService writing) { this.writing=writing; }
    @Override public EnglishContentType type() { return EnglishContentType.WRITING; }
    @Override public ContentDescriptor require(long id) {
        WritingPromptView item=writing.get(id);
        return new ContentDescriptor(type(),item.id(),item.slug(),item.title(),item.summary(),
                item.cefrLevel(),item.coverUrl(),item.publishStatus(),item.sortOrder()==null?0:item.sortOrder());
    }
    @Override public ContentCatalogSlice catalog(ContentCatalogFilter filter, int limit) {
        return writing.catalogDescriptors(filter, limit);
    }
}
