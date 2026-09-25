package com.starrainnotes.english.reading.application;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.english.shared.events.EnglishContentChange;
import com.starrainnotes.english.shared.events.EnglishContentChangeType;
import com.starrainnotes.english.shared.events.EnglishContentKind;
import com.starrainnotes.english.reading.dto.ReadingArticleRequest;
import com.starrainnotes.english.reading.dto.ReadingArticleView;
import com.starrainnotes.english.reading.entity.ReadingArticle;
import com.starrainnotes.english.reading.support.ReadingTextStatistics;
import com.starrainnotes.english.reading.infrastructure.ReadingRepository;
import com.starrainnotes.english.reading.domain.ReadingPublishPolicy;
import com.starrainnotes.account.review.api.ReviewSubmissionResult;
import com.starrainnotes.english.api.MediaPort;
import com.starrainnotes.english.shared.exercise.api.EnglishExerciseRemovalPort;
import com.starrainnotes.english.shared.review.EnglishUpdateReview;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

/** Transaction boundaries for reading article mutations. */
@Service
public class ReadingCommandService {
    private static final String DRAFT = "DRAFT";
    private static final String PUBLISHED = "PUBLISHED";
    private final ReadingRepository repository;
    private final ReadingRelationService relations;
    private final ReadingPublishPolicy policy;
    private final MediaPort media;
    private final EnglishExerciseRemovalPort exerciseRemoval;
    private final EnglishUpdateReview reviews;
    public ReadingCommandService(ReadingRepository repository, ReadingRelationService relations,
                                 ReadingPublishPolicy policy, MediaPort media,
                                 EnglishExerciseRemovalPort exerciseRemoval, EnglishUpdateReview reviews) {
        this.repository = repository;
        this.relations = relations;
        this.policy = policy;
        this.media = media;
        this.exerciseRemoval = exerciseRemoval;
        this.reviews = reviews;
    }

    @Transactional
    public ReadingArticleView create(ReadingArticleRequest request) {
        ReadingTextStatistics.Stats stats = ReadingTextStatistics.analyze(request.bodyMarkdown());
        String slug = NumericSlugGenerator.forCreate(request.slug(), candidate -> repository.slugExists(candidate, null));
        assertSlugFree(slug, null);
        validateCefr(request.cefrLevel());
        policy.requireLevel(request.readingLevel());
        media.requireImageIfPresent(request.coverMediaId());
        ReadingArticle article = new ReadingArticle();
        applyFields(article, request, stats, slug);
        article.setPublishStatus(DRAFT);
        if (article.getSortOrder() == null) article.setSortOrder(repository.nextSortOrder());
        try { repository.insert(article); } catch (DuplicateKeyException ex) { throw slugConflict(); }
        relations.replaceRelations(article.getId(), request);
        return repository.get(article.getId());
    }

    public ReviewSubmissionResult updateForEditor(Long actorId, boolean superAdmin, Long id, ReadingArticleRequest request) {
        return reviews.decide(superAdmin, "ENGLISH_READING_ARTICLE", id, request.title(), request, actorId,
                () -> update(id, request));
    }

    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.READING, changeType = EnglishContentChangeType.UPDATED)
    public ReadingArticleView update(Long id, ReadingArticleRequest request) {
        ReadingArticle article = repository.require(id);
        ReadingTextStatistics.Stats stats = ReadingTextStatistics.analyze(request.bodyMarkdown());
        String slug = NumericSlugGenerator.forUpdate(request.slug(), article.getSlug());
        assertSlugFree(slug, id);
        validateCefr(request.cefrLevel());
        policy.requireLevel(request.readingLevel());
        media.requireImageIfPresent(request.coverMediaId());
        applyFields(article, request, stats, slug);
        try { repository.update(article); } catch (DuplicateKeyException ex) { throw slugConflict(); }
        relations.replaceRelations(id, request);
        return repository.get(id);
    }

    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.READING, changeType = EnglishContentChangeType.PUBLISHED)
    public ReadingArticleView publish(Long id) {
        ReadingArticle article = repository.require(id);
        List<String> problems = policy.violations(article,
                relations.hasEnabledDimension(id, "TOPIC"), relations.hasEnabledDimension(id, "GENRE"));
        if (!problems.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_READING_PUBLISH_INVALID",
                    "Cannot publish", String.join("; ", problems));
        }
        repository.publish(id);
        return repository.get(id);
    }

    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.READING, changeType = EnglishContentChangeType.WITHDRAWN)
    public ReadingArticleView withdraw(Long id) {
        ReadingArticle article = repository.require(id);
        if (DRAFT.equals(article.getPublishStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_INVALID_PUBLISH_TRANSITION",
                    "Invalid publish transition", "A draft article cannot be withdrawn.");
        }
        repository.withdraw(id);
        return repository.get(id);
    }

    @Transactional
    public void delete(Long id) {
        ReadingArticle article = repository.require(id);
        if (PUBLISHED.equals(article.getPublishStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "ENGLISH_READING_PUBLISHED_DELETE_FORBIDDEN",
                    "Published article cannot be deleted", "Withdraw the article before deleting it.");
        }
        List<Long> exerciseIds = repository.boundExerciseIds(id);
        repository.delete(id);
        exerciseRemoval.deleteAll(exerciseIds);
    }

    private void validateCefr(String cefr) {
        if (cefr != null && !repository.cefrExists(cefr)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_CEFR_INVALID",
                    "Invalid CEFR level", "The selected CEFR level does not exist.");
        }
    }
    private void assertSlugFree(String slug, Long excludedId) {
        if (repository.slugExists(slug, excludedId)) throw slugConflict();
    }
    private ApiException slugConflict() {
        return new ApiException(HttpStatus.CONFLICT, "ENGLISH_CONTENT_SLUG_CONFLICT",
                "Slug already in use", "Choose another stable slug.");
    }
    private void applyFields(ReadingArticle article, ReadingArticleRequest request,
                             ReadingTextStatistics.Stats stats, String slug) {
        article.setTitle(request.title().trim());
        article.setSlug(slug);
        article.setSummary(request.summary().trim());
        article.setBodyMarkdown(request.bodyMarkdown());
        article.setCoverMediaId(request.coverMediaId());
        article.setReadingLevel(request.readingLevel());
        article.setCefrLevel(request.cefrLevel());
        article.setSourceName(clean(request.sourceName()));
        article.setSourceUrl(clean(request.sourceUrl()));
        article.setCopyrightNote(clean(request.copyrightNote()));
        article.setWordCount(stats.wordCount());
        article.setUniqueWordCount(stats.uniqueWordCount());
        article.setAverageSentenceWords(BigDecimal.valueOf(stats.averageSentenceWords()));
        article.setMaxSentenceWords(stats.maxSentenceWords());
        article.setEstimatedMinutes(stats.estimatedMinutes());
        if (request.sortOrder() != null) article.setSortOrder(request.sortOrder());
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
