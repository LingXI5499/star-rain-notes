package com.starrainnotes.english.grammar.application;

import com.starrainnotes.english.grammar.dto.GrammarLessonDetailView;
import com.starrainnotes.english.shared.content.ContentDescriptor;
import com.starrainnotes.english.shared.content.EnglishContentDescriptorProvider;
import com.starrainnotes.english.shared.content.EnglishContentType;
import org.springframework.stereotype.Component;

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
}
