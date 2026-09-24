package com.starrainnotes.english.shared.bundle.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.english.shared.bundle.dto.BundleRequest;
import com.starrainnotes.english.shared.bundle.dto.BundleView;
import com.starrainnotes.english.shared.bundle.entity.EnglishLearningBundle;
import com.starrainnotes.english.shared.bundle.infrastructure.LearningBundleRepository;
import com.starrainnotes.english.api.MediaPort;
import com.starrainnotes.english.shared.events.EnglishContentChange;
import com.starrainnotes.english.shared.events.EnglishContentChangeType;
import com.starrainnotes.english.shared.events.EnglishContentKind;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Learning bundle lifecycle and publication rules. */
@Service
public class LearningBundleService {
    private static final String DRAFT = "DRAFT";
    private final LearningBundleRepository repository;
    private final LearningBundleItemService items;
    private final MediaPort media;

    public LearningBundleService(LearningBundleRepository repository, LearningBundleItemService items,
                                 MediaPort media) {
        this.repository = repository;
        this.items = items;
        this.media = media;
    }

    public List<BundleView> list() { return repository.list(); }

    public List<BundleView> publicList() {
        return repository.publishedList().stream()
                .filter(bundle -> items.publiclyAccessible(bundle.id())).toList();
    }

    public BundleView get(Long id) { return requireDetail(id); }

    public BundleView publicGet(String slug) {
        BundleView bundle = repository.publishedBySlug(slug);
        if (bundle == null || !items.publiclyAccessible(bundle.id())) throw contentNotPublished();
        return bundle;
    }

    @Transactional
    public BundleView create(BundleRequest request) {
        String slug = NumericSlugGenerator.forCreate(request.slug(), candidate -> repository.slugExists(candidate, null));
        validateSlugFree(slug, null);
        validateCefr(request.primaryCefr());
        validateCover(request.coverMediaId());
        int order = request.sortOrder() == null ? repository.nextOrder() : request.sortOrder();
        EnglishLearningBundle bundle = new EnglishLearningBundle();
        bundle.setTitle(request.title().trim());
        bundle.setSlug(slug);
        bundle.setSummary(clean(request.summary()));
        bundle.setPrimaryCefr(request.primaryCefr());
        bundle.setCoverMediaId(request.coverMediaId());
        bundle.setPublishStatus(DRAFT);
        bundle.setSortOrder(order);
        try {
            repository.insert(bundle);
        } catch (DuplicateKeyException ex) {
            throw slugConflict();
        }
        return requireDetail(bundle.getId());
    }

    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.BUNDLE, changeType = EnglishContentChangeType.UPDATED)
    public BundleView update(Long id, BundleRequest request) {
        EnglishLearningBundle bundle = requireEntity(id);
        String slug = NumericSlugGenerator.forUpdate(request.slug(), bundle.getSlug());
        validateSlugFree(slug, id);
        validateCefr(request.primaryCefr());
        validateCover(request.coverMediaId());
        bundle.setTitle(request.title().trim());
        bundle.setSlug(slug);
        bundle.setSummary(clean(request.summary()));
        bundle.setPrimaryCefr(request.primaryCefr());
        bundle.setCoverMediaId(request.coverMediaId());
        if (request.sortOrder() != null) bundle.setSortOrder(request.sortOrder());
        try {
            repository.update(bundle);
        } catch (DuplicateKeyException ex) {
            throw slugConflict();
        }
        return requireDetail(id);
    }

    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.BUNDLE, changeType = EnglishContentChangeType.PUBLISHED)
    public BundleView publish(Long id) {
        requireEntity(id);
        items.assertPublishable(id);
        repository.publish(id);
        return requireDetail(id);
    }

    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.BUNDLE, changeType = EnglishContentChangeType.WITHDRAWN)
    public BundleView withdraw(Long id) {
        if (DRAFT.equals(requireEntity(id).getPublishStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_INVALID_PUBLISH_TRANSITION",
                    "Invalid publish transition", "A draft bundle cannot be withdrawn.");
        }
        repository.withdraw(id);
        return requireDetail(id);
    }

    @Transactional
    public void delete(Long id) {
        requireEntity(id);
        repository.delete(id);
    }

    private void validateSlugFree(String slug, Long excludedId) {
        if (repository.slugExists(slug, excludedId)) throw slugConflict();
    }

    private void validateCefr(String cefr) {
        if (cefr != null && !repository.cefrExists(cefr)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_CEFR_INVALID",
                    "Invalid CEFR level", "The selected CEFR level does not exist.");
        }
    }

    private void validateCover(Long mediaId) {
        if (mediaId != null && !media.isImage(mediaId)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_COVER_MEDIA",
                    "Invalid cover", "The selected cover must be an existing image asset.");
        }
    }

    private BundleView requireDetail(Long id) {
        BundleView bundle = repository.detail(id);
        if (bundle == null) throw bundleNotFound();
        return bundle;
    }

    private EnglishLearningBundle requireEntity(Long id) {
        EnglishLearningBundle bundle = repository.entity(id);
        if (bundle == null) throw bundleNotFound();
        return bundle;
    }

    private ApiException bundleNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_BUNDLE_NOT_FOUND",
                "Bundle not found", "The learning bundle does not exist.");
    }

    private ApiException slugConflict() {
        return new ApiException(HttpStatus.CONFLICT, "ENGLISH_CONTENT_SLUG_CONFLICT",
                "Slug already in use", "Choose another stable slug.");
    }

    private ApiException contentNotPublished() {
        return new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_CONTENT_NOT_PUBLISHED",
                "Bundle not available", "The requested learning bundle is not published or is incomplete.");
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
