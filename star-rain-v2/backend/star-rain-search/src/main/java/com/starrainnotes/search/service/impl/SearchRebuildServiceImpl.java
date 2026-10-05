package com.starrainnotes.search.service.impl;

import com.starrainnotes.blog.api.BlogSearchSourceApi;
import com.starrainnotes.blog.api.dto.BlogDocumentPage;
import com.starrainnotes.blog.api.dto.BlogPostDocument;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.api.PortfolioPublicApi;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;
import com.starrainnotes.profile.api.ProfilePublicApi;
import com.starrainnotes.profile.api.dto.ProfilePublishedDocument;
import com.starrainnotes.search.api.SearchIndexApi;
import com.starrainnotes.search.api.dto.SearchableDocument;
import com.starrainnotes.search.service.SearchIndexService;
import com.starrainnotes.search.service.SearchRebuildService;
import com.starrainnotes.search.mapper.SearchDocumentMapper;
import com.starrainnotes.search.exception.SearchTypeInvalidException;
import com.starrainnotes.search.utils.SearchTextExtractor;
import com.starrainnotes.tutorial.content.api.TutorialChapterDocument;
import com.starrainnotes.tutorial.content.api.TutorialDocumentSourceApi;
import com.starrainnotes.tutorial.content.api.TutorialPublishedDocument;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
public class SearchRebuildServiceImpl implements SearchRebuildService {
    private static final int BATCH = 100;
    private final BlogSearchSourceApi blogs;
    private final TutorialDocumentSourceApi tutorials;
    private final PortfolioPublicApi works;
    private final ProfilePublicApi profiles;
    private final SearchIndexApi index;
    private final SearchDocumentMapper mapper;
    private final SearchTextExtractor extractor;

    @Override
    public long rebuild(String type) {
        if (type == null || type.isBlank()) rebuildAll();
        else {
            if (!SearchIndexService.TYPES.contains(type)) throw new SearchTypeInvalidException();
            rebuildType(type);
        }
        return mapper.activeCount();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rebuildAll() {
        rebuildTutorials();
        rebuildBlogs();
        rebuildWorks();
        rebuildProfile();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rebuildType(String type) {
        if (!SearchIndexService.TYPES.contains(type)) throw new IllegalArgumentException("Invalid search type");
        switch (type) {
            case "TUTORIAL", "CHAPTER" -> rebuildTutorials();
            case "BLOG" -> rebuildBlogs();
            case "PORTFOLIO" -> rebuildWorks();
            case "PROFILE" -> rebuildProfile();
            default -> throw new IllegalArgumentException("Invalid search type");
        }
    }

    @Override
    public void indexBlog(BlogPostDocument post) {
        index.upsert(document("BLOG", post.getId(), post.getTitle(), post.getSummary(),
            post.getBodyMarkdown(), "/blog/posts/" + post.getSlug(), post.getPublishedAt(), post.getUpdatedAt()));
    }

    @Override
    public void indexWork(PortfolioPublishedWork work) {
        index.upsert(document("PORTFOLIO", work.getId(), work.getTitle(), work.getSummary(),
            work.getBodyMarkdown(), "/portfolio/" + work.getSlug(), work.getPublishedAt(), work.getUpdatedAt()));
    }

    private void rebuildBlogs() {
        Set<String> seen = new HashSet<>();
        Long cursor = null;
        do {
            BlogDocumentPage result = blogs.listPublishedDocuments(cursor, BATCH);
            for (BlogPostDocument post : result.getItems()) {
                indexBlog(post);
                seen.add(key("BLOG", post.getId()));
            }
            cursor = result.getNextCursor();
        } while (cursor != null);
        removeStale("BLOG", seen);
    }

    private void rebuildTutorials() {
        Set<String> seenTutorials = new HashSet<>();
        Set<String> seenChapters = new HashSet<>();
        for (int page = 1; ; page++) {
            PageResult<TutorialPublishedDocument> result = tutorials.page(page, BATCH);
            for (TutorialPublishedDocument detail : result.getItems()) {
                index.upsert(document("TUTORIAL", detail.getId(), detail.getTitle(), detail.getSummary(), "",
                    "/tutorials/" + detail.getSlug(), detail.getPublishedAt(), detail.getPublishedAt()));
                seenTutorials.add(key("TUTORIAL", detail.getId()));
                for (TutorialChapterDocument chapter : detail.getChapters()) {
                    index.upsert(document("CHAPTER", chapter.getId(), chapter.getTitle(), chapter.getSummary(),
                        chapter.getSearchableText(), "/tutorials/" + detail.getSlug() + "/" + chapter.getSlug(),
                        detail.getPublishedAt(), detail.getPublishedAt()));
                    seenChapters.add(key("CHAPTER", chapter.getId()));
                }
            }
            if ((long) page * BATCH >= result.getTotal()) break;
        }
        removeStale("TUTORIAL", seenTutorials);
        removeStale("CHAPTER", seenChapters);
    }

    private void rebuildWorks() {
        Set<String> seen = new HashSet<>();
        for (int page = 1; ; page++) {
            PageResult<PortfolioPublishedWork> result = works.publishedWorks(page, BATCH);
            for (PortfolioPublishedWork item : result.getItems()) {
                indexWork(item);
                seen.add(key("PORTFOLIO", item.getId()));
            }
            if ((long) page * BATCH >= result.getTotal()) break;
        }
        removeStale("PORTFOLIO", seen);
    }

    private void rebuildProfile() {
        ProfilePublishedDocument profile = profiles.publishedDocument();
        Set<String> seen = new HashSet<>();
        if (profile != null) {
            index.upsert(document("PROFILE", profile.getId(), profile.getTitle(), profile.getSummary(),
                profile.getBodyMarkdown(),
                "/about", null, LocalDateTime.now(ZoneOffset.UTC)));
            seen.add(key("PROFILE", profile.getId()));
        }
        removeStale("PROFILE", seen);
    }

    private void removeStale(String type, Set<String> seen) {
        for (String key : mapper.activeKeys(type)) {
            if (!seen.contains(key)) index.remove(key);
        }
    }

    private SearchableDocument document(String type, Long id, String title, String summary,
        String markdown, String route, LocalDateTime publishedAt, LocalDateTime updatedAt) {
        SearchableDocument result = new SearchableDocument();
        result.setContentType(type);
        result.setContentId(id);
        result.setDocumentKey(key(type, id));
        result.setTitle(title);
        result.setSummary(summary);
        result.setSearchableText(extractor.plainText(markdown));
        result.setRoutePath(route);
        result.setPublishedAt(publishedAt);
        result.setSourceUpdatedAt(updatedAt == null ? LocalDateTime.now(ZoneOffset.UTC) : updatedAt);
        return result;
    }

    private String key(String type, Long id) { return type + ":" + id; }
}
