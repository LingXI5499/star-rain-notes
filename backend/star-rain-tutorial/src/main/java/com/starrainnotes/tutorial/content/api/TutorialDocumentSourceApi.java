package com.starrainnotes.tutorial.content.api;

import com.starrainnotes.common.result.PageResult;
import java.util.Optional;

/** Public documents for consumers such as Search and SEO; never exposes drafts. */
public interface TutorialDocumentSourceApi {
    PageResult<TutorialPublishedDocument> page(int page, int pageSize);
    Optional<TutorialPublishedDocument> bySlug(String slug);
    Optional<TutorialChapterDocument> chapter(String tutorialSlug, String chapterSlug);
}
