package com.starrainnotes.search.rebuild;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.blog.dto.BlogPublicQueryDTO;
import com.starrainnotes.blog.service.BlogPublicService;
import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;
import com.starrainnotes.blog.vo.BlogPostPublicVO;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.service.PortfolioWorkService;
import com.starrainnotes.portfolio.vo.WorkVO;
import com.starrainnotes.profile.api.ProfilePublicApi;
import com.starrainnotes.profile.vo.ProfileVO;
import com.starrainnotes.search.api.SearchIndexApi;
import com.starrainnotes.search.api.SearchableDocument;
import com.starrainnotes.search.index.SearchIndexService;
import com.starrainnotes.search.mapper.SearchDocumentMapper;
import com.starrainnotes.search.text.SearchTextExtractor;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
public class SearchRebuildService {
    private static final int BATCH = 100;
    private final BlogPublicService blogs;
    private final TutorialPublicationService tutorials;
    private final PortfolioWorkService works;
    private final ProfilePublicApi profiles;
    private final SearchIndexApi index;
    private final SearchDocumentMapper mapper;
    private final SearchTextExtractor extractor;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rebuildAll() {
        rebuildTutorials();
        rebuildBlogs();
        rebuildWorks();
        rebuildProfile();
    }

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

    public void indexBlog(BlogPostPublicDetailVO post) {
        index.upsert(document("BLOG", post.getId(), post.getTitle(), post.getSummary(),
            post.getBodyMarkdown(), "/blog/posts/" + post.getSlug(), post.getPublishedAt(), post.getUpdatedAt()));
    }

    public void indexWork(WorkVO work) {
        index.upsert(document("PORTFOLIO", Long.valueOf(work.getId()), work.getTitle(), work.getSummary(),
            work.getBodyMarkdown(), "/portfolio/" + work.getSlug(), work.getPublishedAt(), work.getUpdatedAt()));
    }

    private void rebuildBlogs() {
        Set<String> seen = new HashSet<>();
        for (int page = 1; ; page++) {
            BlogPublicQueryDTO query = new BlogPublicQueryDTO();
            query.setPage(page);
            query.setPageSize(BATCH);
            PageResult<BlogPostPublicVO> result = blogs.listPosts(query);
            for (BlogPostPublicVO item : result.getItems()) {
                BlogPostPublicDetailVO detail = blogs.postBySlug(item.getSlug());
                indexBlog(detail);
                seen.add(key("BLOG", detail.getId()));
            }
            if ((long) page * BATCH >= result.getTotal()) break;
        }
        removeStale("BLOG", seen);
    }

    private void rebuildTutorials() {
        Set<String> seenTutorials = new HashSet<>();
        Set<String> seenChapters = new HashSet<>();
        for (int page = 1; ; page++) {
            PageResult<JsonNode> result = tutorials.publicTutorials(null, null, page, BATCH);
            for (JsonNode item : result.getItems()) {
                String slug = item.path("slug").asText();
                JsonNode detail = tutorials.publicTutorial(slug);
                Long tutorialId = id(detail);
                LocalDateTime publishedAt = date(detail.path("publishedAt").asText());
                index.upsert(document("TUTORIAL", tutorialId, detail.path("title").asText(),
                    detail.path("summary").asText(), "", "/tutorials/" + slug, publishedAt, publishedAt));
                seenTutorials.add(key("TUTORIAL", tutorialId));
                for (JsonNode group : detail.path("groups")) {
                    for (JsonNode chapter : group.path("chapters")) {
                        String chapterSlug = chapter.path("slug").asText();
                        JsonNode body = tutorials.publicChapter(slug, chapterSlug);
                        Long chapterId = id(body);
                        index.upsert(document("CHAPTER", chapterId, body.path("title").asText(),
                            body.path("summary").asText(), publicChapterText(body),
                            "/tutorials/" + slug + "/" + chapterSlug, publishedAt, publishedAt));
                        seenChapters.add(key("CHAPTER", chapterId));
                    }
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
            PageResult<WorkVO> result = works.publicWorks(page, BATCH, null);
            for (WorkVO item : result.getItems()) {
                WorkVO detail = works.publicWorkById(Long.valueOf(item.getId()));
                indexWork(detail);
                seen.add(key("PORTFOLIO", Long.valueOf(detail.getId())));
            }
            if ((long) page * BATCH >= result.getTotal()) break;
        }
        removeStale("PORTFOLIO", seen);
    }

    private void rebuildProfile() {
        Long id = profiles.publicProfileId();
        Set<String> seen = new HashSet<>();
        if (id != null) {
            ProfileVO profile = profiles.summary();
            List<String> extra = new ArrayList<>();
            if (profile.getExperiences() != null) profile.getExperiences().forEach(item -> {
                extra.add(item.getTitle());
                extra.add(item.getOrganization());
                extra.add(item.getDescriptionMd());
            });
            if (profile.getSkills() != null) profile.getSkills().forEach(item -> extra.add(item.getName()));
            String body = String.join(" ", extra.stream().filter(value -> value != null).toList());
            index.upsert(document("PROFILE", id, profile.getDisplayName(), profile.getHeadline(),
                (profile.getBioMarkdown() == null ? "" : profile.getBioMarkdown()) + " " + body,
                "/about", null, LocalDateTime.now(ZoneOffset.UTC)));
            seen.add(key("PROFILE", id));
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
    private Long id(JsonNode node) { return Long.valueOf(node.path("id").asText()); }
    private LocalDateTime date(String value) { return value == null || value.isBlank() ? null : LocalDateTime.parse(value); }

    private String publicChapterText(JsonNode chapter) {
        StringBuilder text = new StringBuilder(chapter.path("bodyMarkdown").asText());
        for (JsonNode card : chapter.path("cards")) {
            text.append(' ').append(card.path("frontText").asText());
            text.append(' ').append(card.path("backMarkdown").asText());
        }
        for (JsonNode question : chapter.path("questions")) {
            text.append(' ').append(question.path("questionText").asText());
        }
        return text.toString();
    }
}
