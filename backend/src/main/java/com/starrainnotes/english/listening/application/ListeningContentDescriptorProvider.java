package com.starrainnotes.english.listening.application;

import com.starrainnotes.english.shared.content.TagMatchCandidate;
import java.util.List;

import com.starrainnotes.english.listening.dto.ListeningItemView;
import com.starrainnotes.english.shared.content.ContentDescriptor;
import com.starrainnotes.english.shared.content.ContentCatalogFilter;
import com.starrainnotes.english.shared.content.ContentCatalogSlice;
import com.starrainnotes.english.shared.content.EnglishContentDescriptorProvider;
import com.starrainnotes.english.shared.content.EnglishContentType;
import org.springframework.stereotype.Component;

@Component
public class ListeningContentDescriptorProvider implements EnglishContentDescriptorProvider {
    private final ListeningQueryService listening;
    public ListeningContentDescriptorProvider(ListeningQueryService listening) { this.listening=listening; }
    @Override public EnglishContentType type() { return EnglishContentType.LISTENING; }
    @Override public List<Long> tagIds(long contentId) { return listening.tagIds(contentId); }
    @Override public List<TagMatchCandidate> tagMatches(List<Long> termIds) { return listening.tagMatches(termIds); }
    @Override public ContentDescriptor require(long id) {
        ListeningItemView item=listening.get(id);
        return new ContentDescriptor(type(),item.id(),item.slug(),item.title(),item.summary(),
                item.cefrLevel(),item.coverUrl(),item.publishStatus(),item.sortOrder()==null?0:item.sortOrder());
    }
    @Override public ContentCatalogSlice catalog(ContentCatalogFilter filter, int limit) {
        return listening.catalogDescriptors(filter, limit);
    }
    @Override public long publishedCount() {
        return catalog(new ContentCatalogFilter("PUBLISHED", null, null), 0).total();
    }
}
