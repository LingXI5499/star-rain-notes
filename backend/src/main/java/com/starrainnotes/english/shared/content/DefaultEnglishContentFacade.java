package com.starrainnotes.english.shared.content;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.api.ContentReadiness;
import com.starrainnotes.english.api.EnglishContentFacade;
import com.starrainnotes.english.api.TaxonomyRef;
import com.starrainnotes.english.shared.taxonomy.application.TaxonomyQueryService;
import com.starrainnotes.english.shared.taxonomy.entity.EnglishTaxonomyTerm;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultEnglishContentFacade implements EnglishContentFacade {
    private final EnglishContentRegistry content;
    private final TaxonomyQueryService taxonomy;

    public DefaultEnglishContentFacade(EnglishContentRegistry content, TaxonomyQueryService taxonomy) {
        this.content = content;
        this.taxonomy = taxonomy;
    }

    @Override public ContentDescriptor requirePublished(EnglishContentType type, long contentId) {
        return content.requirePublished(type, contentId);
    }

    @Override public List<ContentDescriptor> listPublished(EnglishContentType type) {
        return content.publishedCandidates(type);
    }

    @Override public ContentReadiness readiness(EnglishContentType type, long contentId) {
        ContentDescriptor descriptor = content.require(type, contentId);
        if (!descriptor.published()) {
            return new ContentReadiness(descriptor, false, List.of("CONTENT_UNPUBLISHED"));
        }
        try {
            content.requirePublished(type, contentId);
            return new ContentReadiness(descriptor, true, List.of());
        } catch (ApiException ex) {
            return new ContentReadiness(descriptor, false, List.of("DEPENDENCY_UNPUBLISHED"));
        }
    }

    @Override public List<TaxonomyRef> taxonomy(EnglishContentType type, long contentId) {
        content.require(type, contentId);
        return content.tagIds(type, contentId).stream()
                .map(id -> taxonomy.require(id))
                .map(this::toRef)
                .toList();
    }

    private TaxonomyRef toRef(EnglishTaxonomyTerm term) {
        return new TaxonomyRef(term.getId(), term.getDimension(), term.getName(),
                term.getSlug(), Boolean.TRUE.equals(term.getEnabled()));
    }
}
