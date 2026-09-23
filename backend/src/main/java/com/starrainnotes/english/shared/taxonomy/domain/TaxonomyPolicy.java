package com.starrainnotes.english.shared.taxonomy.domain;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.shared.taxonomy.entity.EnglishTaxonomyTerm;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Taxonomy hierarchy and request rules, independent of persistence. */
@Component
public class TaxonomyPolicy {
    public static final Set<String> DIMENSIONS =
            Set.of("TOPIC", "SCENE", "FUNCTION", "ABILITY", "GENRE", "FORMAT");

    public String requireDimension(String dimension) {
        if (dimension == null || !DIMENSIONS.contains(dimension.trim())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_DEPTH_INVALID",
                    "Invalid dimension", "Dimension must be one of TOPIC/SCENE/FUNCTION/ABILITY/GENRE/FORMAT.");
        }
        return dimension.trim();
    }

    public void validateParentAssignment(Long id, Long parentId, boolean hasChildren) {
        if (id != null && id.equals(parentId)) {
            throw depthInvalid("A taxonomy term cannot be its own parent.");
        }
        if (hasChildren && parentId != null) {
            throw depthInvalid("A taxonomy term that has children must stay a root.");
        }
    }

    public void validateParent(String dimension, EnglishTaxonomyTerm parent) {
        if (!parent.getDimension().equals(dimension)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_DEPTH_INVALID",
                    "Cross-dimension parent", "A child term must share its parent's dimension.");
        }
        if (parent.getParentId() != null) {
            throw depthInvalid("A taxonomy term can be at most two levels deep.");
        }
    }

    public void validateDimensionChange(EnglishTaxonomyTerm term, String dimension, boolean hasChildren) {
        if (hasChildren && !dimension.equals(term.getDimension())) {
            throw depthInvalid("A taxonomy term that has children cannot change dimension.");
        }
    }

    public void validateSortOrder(Integer sortOrder) {
        if (sortOrder != null && sortOrder < 1) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_DEPTH_INVALID",
                    "Invalid sort order", "Sort order must be a positive integer.");
        }
    }

    public void assertSlugFree(boolean exists) {
        if (exists) throw slugConflict();
    }

    public ApiException slugConflict() {
        return new ApiException(HttpStatus.CONFLICT, "ENGLISH_CONTENT_SLUG_CONFLICT",
                "Slug already in use", "Choose another stable slug.");
    }

    public void assertDeletable(boolean hasChildren, boolean referenced) {
        if (hasChildren) {
            throw new ApiException(HttpStatus.CONFLICT, "ENGLISH_TAXONOMY_IN_USE",
                    "Taxonomy term in use", "This term still has children. Move or delete them first.");
        }
        if (referenced) {
            throw new ApiException(HttpStatus.CONFLICT, "ENGLISH_TAXONOMY_IN_USE",
                    "Taxonomy term in use",
                    "This term is referenced by published content and cannot be deleted.");
        }
    }

    private ApiException depthInvalid(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_DEPTH_INVALID",
                "Invalid taxonomy hierarchy", detail);
    }
}
