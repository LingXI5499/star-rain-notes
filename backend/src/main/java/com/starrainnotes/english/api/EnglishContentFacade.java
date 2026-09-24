package com.starrainnotes.english.api;

import com.starrainnotes.english.shared.content.ContentDescriptor;
import com.starrainnotes.english.shared.content.EnglishContentType;
import java.util.List;

public interface EnglishContentFacade {
    ContentDescriptor requirePublished(EnglishContentType type, long contentId);
    List<ContentDescriptor> listPublished(EnglishContentType type);
    ContentReadiness readiness(EnglishContentType type, long contentId);
    List<TaxonomyRef> taxonomy(EnglishContentType type, long contentId);
}
