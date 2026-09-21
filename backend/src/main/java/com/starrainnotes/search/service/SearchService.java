package com.starrainnotes.search.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.search.dto.SearchCountsView;
import com.starrainnotes.search.dto.SearchItemView;
import com.starrainnotes.search.dto.SearchPageView;
import com.starrainnotes.search.repository.SearchDocument;
import com.starrainnotes.search.repository.SearchRepository;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Global-search validation, ranking, type filtering, pagination and response assembly. */
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final int MAX_PAGE_SIZE = 50;

    private final SearchRepository repository;
    private final SiteSettingsTimezone timezone;

    public SearchPageView search(String rawQuery, String type, int page, int pageSize) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() < 2 || query.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "Invalid search query", "q must be between 2 and 100 characters after trimming.");
        }
        String pattern = "%" + escapeLike(query) + "%";
        List<Candidate> tutorials = candidates(repository.tutorials(pattern), query);
        List<Candidate> chapters = candidates(repository.chapters(pattern), query);
        List<Candidate> blogs = candidates(repository.blogs(pattern), query);
        List<Candidate> portfolios = candidates(repository.portfolios(pattern), query);
        List<Candidate> grammar = candidates(repository.grammar(pattern), query);
        List<Candidate> reading = candidates(repository.reading(pattern), query);
        List<Candidate> listening = candidates(repository.listeningMaterials(pattern), query);
        List<Candidate> pronunciation = candidates(repository.pronunciationRules(pattern), query);
        List<Candidate> writing = candidates(repository.writing(pattern), query);
        List<Candidate> words = candidates(repository.vocabulary(pattern), query);

        SearchCountsView counts = new SearchCountsView(
                tutorials.size(), chapters.size(), blogs.size(), portfolios.size(), grammar.size(), reading.size(),
                listening.size() + pronunciation.size(), writing.size(), words.size());
        List<Candidate> all = selectedCandidates(type, tutorials, chapters, blogs, portfolios, grammar, reading,
                listening, pronunciation, writing, words);
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

    private List<Candidate> candidates(List<SearchDocument> documents, String query) {
        return documents.stream().map(document -> new Candidate(
                document.type(), document.id(), document.title(), document.summary(), document.slug(),
                document.tutorialSlug(), document.chapterSlug(), document.activityAt(),
                score(query, document.title(), document.summary(), document.body()))).toList();
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
