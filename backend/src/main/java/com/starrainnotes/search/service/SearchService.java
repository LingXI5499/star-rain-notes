package com.starrainnotes.search.service;

import com.starrainnotes.blog.api.BlogSearchPort;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.grammar.api.GrammarSearchPort;
import com.starrainnotes.english.listening.api.ListeningSearchPort;
import com.starrainnotes.english.reading.api.ReadingSearchPort;
import com.starrainnotes.english.vocabulary.api.VocabularySearchPort;
import com.starrainnotes.english.writing.api.WritingSearchPort;
import com.starrainnotes.portfolio.api.PortfolioSearchPort;
import com.starrainnotes.search.dto.SearchCountsView;
import com.starrainnotes.search.dto.SearchItemView;
import com.starrainnotes.search.dto.SearchPageView;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import com.starrainnotes.tutorial.api.TutorialSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Global-search validation, ranking, type filtering, pagination and response assembly. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final int MAX_PAGE_SIZE = 50;

    private final TutorialSearchPort tutorials;
    private final BlogSearchPort blogs;
    private final PortfolioSearchPort portfolios;
    private final GrammarSearchPort grammar;
    private final ReadingSearchPort reading;
    private final ListeningSearchPort listening;
    private final WritingSearchPort writing;
    private final VocabularySearchPort vocabulary;
    private final SiteSettingsTimezone timezone;

    public SearchPageView search(String rawQuery, String type, int page, int pageSize) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() < 2 || query.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "Invalid search query", "q must be between 2 and 100 characters after trimming.");
        }
        String pattern = "%" + escapeLike(query) + "%";
        List<Candidate> tutorialHits = tutorials.tutorials(pattern).stream()
                .map(hit -> candidate("TUTORIAL", hit.id(), hit.title(), hit.summary(), null, hit.slug(),
                        null, null, hit.updatedAt(), query))
                .toList();
        List<Candidate> chapterHits = tutorials.chapters(pattern).stream()
                .map(hit -> candidate("CHAPTER", hit.id(), hit.title(), hit.summary(), hit.body(), null,
                        hit.tutorialSlug(), hit.chapterSlug(), hit.updatedAt(), query))
                .toList();
        List<Candidate> blogHits = blogs.search(pattern).stream()
                .map(hit -> candidate("BLOG", hit.id(), hit.title(), hit.summary(), hit.body(), hit.slug(),
                        null, null, hit.publishedAt(), query))
                .toList();
        List<Candidate> portfolioHits = portfolios.search(pattern).stream()
                .map(hit -> candidate("PORTFOLIO", hit.id(), hit.title(), hit.summary(), hit.body(), hit.slug(),
                        null, null, hit.updatedAt(), query))
                .toList();
        List<Candidate> grammarHits = grammar.search(pattern).stream()
                .map(hit -> content("GRAMMAR", hit, query)).toList();
        List<Candidate> readingHits = reading.search(pattern).stream()
                .map(hit -> content("READING", hit.id(), hit.title(), hit.summary(), hit.body(), hit.slug(), hit.updatedAt(), query))
                .toList();
        List<Candidate> listeningHits = listening.materials(pattern).stream()
                .map(hit -> content("LISTENING", hit.id(), hit.title(), hit.summary(), hit.body(), hit.slug(), hit.updatedAt(), query))
                .toList();
        List<Candidate> pronunciationHits = listening.pronunciationRules(pattern).stream()
                .map(hit -> content("PRONUNCIATION", hit.id(), hit.title(), hit.summary(), hit.body(), hit.slug(), hit.updatedAt(), query))
                .toList();
        List<Candidate> writingHits = new ArrayList<>();
        writing.resources(pattern).stream()
                .map(hit -> candidate("WRITING", hit.id(), hit.title(), hit.summary(), hit.body(), hit.slug(),
                        "resource", null, hit.updatedAt(), query))
                .forEach(writingHits::add);
        writing.prompts(pattern).stream()
                .map(hit -> candidate("WRITING", hit.id(), hit.title(), hit.summary(),
                        writingBody(hit.background(), hit.requirements()), hit.slug(),
                        "practice", null, hit.updatedAt(), query))
                .forEach(writingHits::add);
        List<Candidate> wordHits = vocabulary.search(pattern).stream()
                .map(hit -> candidate("WORD", hit.id(), hit.word(), vocabularySummary(hit.translation(), hit.themeName()),
                        hit.inflections(), null, String.valueOf(hit.themeId()), null, hit.updatedAt(), query))
                .toList();

        SearchCountsView counts = new SearchCountsView(
                tutorialHits.size(), chapterHits.size(), blogHits.size(), portfolioHits.size(), grammarHits.size(),
                readingHits.size(), listeningHits.size() + pronunciationHits.size(), writingHits.size(), wordHits.size());
        List<Candidate> all = selectedCandidates(type, tutorialHits, chapterHits, blogHits, portfolioHits, grammarHits,
                readingHits, listeningHits, pronunciationHits, writingHits, wordHits);
        all.sort(Comparator.comparingInt(Candidate::score).reversed()
                .thenComparing(Candidate::activityAt, Comparator.reverseOrder())
                .thenComparing(Candidate::id, Comparator.reverseOrder()));

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int total = all.size();
        List<SearchItemView> items = all.stream().skip((long) (safePage - 1) * safeSize).limit(safeSize)
                .map(this::toView).toList();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return new SearchPageView(items, counts, total, safePage, safeSize, totalPages);
    }

    private List<Candidate> selectedCandidates(String type, List<Candidate> tutorials, List<Candidate> chapters,
                                               List<Candidate> blogs, List<Candidate> portfolios, List<Candidate> grammar,
                                               List<Candidate> reading, List<Candidate> listening, List<Candidate> pronunciation,
                                               List<Candidate> writing, List<Candidate> words) {
        boolean allTypes = type == null || type.isBlank();
        List<Candidate> result = new ArrayList<>();
        if (allTypes || "tutorial".equals(type)) { result.addAll(tutorials); result.addAll(chapters); }
        if (allTypes || "blog".equals(type)) result.addAll(blogs);
        if (allTypes || "portfolio".equals(type)) result.addAll(portfolios);
        if (allTypes || "grammar".equals(type)) result.addAll(grammar);
        if (allTypes || "reading".equals(type)) result.addAll(reading);
        if (allTypes || "listening".equals(type)) { result.addAll(listening); result.addAll(pronunciation); }
        if (allTypes || "writing".equals(type)) result.addAll(writing);
        if (allTypes || "word".equals(type)) result.addAll(words);
        return result;
    }

    private Candidate content(String type, GrammarSearchPort.Hit hit, String query) {
        return content(type, hit.id(), hit.title(), hit.summary(), hit.body(), hit.slug(), hit.updatedAt(), query);
    }

    private Candidate content(String type, long id, String title, String summary, String body, String slug,
                              LocalDateTime updatedAt, String query) {
        return candidate(type, id, title, summary, body, slug, null, null, updatedAt, query);
    }

    private Candidate candidate(String type, long id, String title, String summary, String body, String slug,
                                String tutorialSlug, String chapterSlug, LocalDateTime activityAt, String query) {
        return new Candidate(type, id, title, summary, slug, tutorialSlug, chapterSlug, activityAt,
                score(query, title, summary, body));
    }

    private String vocabularySummary(String translation, String themeName) {
        return themeName == null || themeName.isBlank() ? translation : translation + " · " + themeName;
    }

    private String writingBody(String background, String requirements) {
        if (background == null || requirements == null) {
            return null;
        }
        return background + "\n" + requirements;
    }

    private int score(String query, String title, String summary, String body) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        String normalizedTitle = title == null ? "" : title.toLowerCase(Locale.ROOT);
        String normalizedSummary = summary == null ? "" : summary.toLowerCase(Locale.ROOT);
        String normalizedBody = body == null ? "" : body.toLowerCase(Locale.ROOT);
        int score = normalizedTitle.equals(normalizedQuery) ? 100
                : normalizedTitle.startsWith(normalizedQuery) ? 80
                : normalizedTitle.contains(normalizedQuery) ? 60 : 0;
        if (normalizedSummary.contains(normalizedQuery)) score += 30;
        if (normalizedBody.contains(normalizedQuery)) score += 10;
        return score;
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private SearchItemView toView(Candidate candidate) {
        return new SearchItemView(candidate.type(), candidate.id(), candidate.title(), candidate.summary(), candidate.slug(),
                candidate.tutorialSlug(), candidate.chapterSlug(), timezone.atSite(candidate.activityAt()).format(ISO_OFFSET),
                candidate.score());
    }

    private record Candidate(String type, Long id, String title, String summary, String slug,
                             String tutorialSlug, String chapterSlug, LocalDateTime activityAt, int score) {
    }
}
