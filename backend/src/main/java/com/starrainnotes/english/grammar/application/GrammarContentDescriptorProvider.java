package com.starrainnotes.english.grammar.application;

import com.starrainnotes.english.grammar.dto.GrammarLessonDetailView;
import com.starrainnotes.english.shared.content.ContentDescriptor;
import com.starrainnotes.english.shared.content.EnglishContentDescriptorProvider;
import com.starrainnotes.english.shared.content.EnglishContentType;
import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class GrammarContentDescriptorProvider implements EnglishContentDescriptorProvider {
    private final GrammarQueryService grammar;
    public GrammarContentDescriptorProvider(GrammarQueryService grammar) { this.grammar=grammar; }
    @Override public EnglishContentType type() { return EnglishContentType.GRAMMAR; }
    @Override public ContentDescriptor require(long id) {
        GrammarLessonDetailView item=grammar.lesson(id);
        return new ContentDescriptor(type(),item.id(),item.slug(),item.title(),item.summary(),
                null,null,item.publishStatus(),item.sortOrder()==null?0:item.sortOrder());
    }
    @Override public long publishedCount() { return grammar.publishedCount(); }
    @Override public ContentDescriptor requirePublished(long id) {
        ContentDescriptor descriptor = require(id);
        if (!descriptor.published() || !"PUBLISHED".equals(grammar.course().publishStatus())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_CONTENT_NOT_PUBLISHED",
                    "Content unavailable", "The selected English content is not published.");
        }
        return descriptor;
    }
    @Override public List<ContentDescriptor> publishedCandidates() { return grammar.publishedDescriptors(); }
}
