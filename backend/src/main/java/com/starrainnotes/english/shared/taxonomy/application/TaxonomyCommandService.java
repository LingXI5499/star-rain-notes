package com.starrainnotes.english.shared.taxonomy.application;

import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.english.shared.taxonomy.domain.TaxonomyPolicy;
import com.starrainnotes.english.shared.taxonomy.dto.TaxonomyMoveRequest;
import com.starrainnotes.english.shared.taxonomy.dto.TaxonomyRequest;
import com.starrainnotes.english.shared.taxonomy.dto.TaxonomyTermView;
import com.starrainnotes.english.shared.taxonomy.entity.EnglishTaxonomyTerm;
import com.starrainnotes.english.shared.taxonomy.infrastructure.TaxonomyRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaxonomyCommandService {
    private final TaxonomyRepository repository;
    private final TaxonomyQueryService queries;
    private final TaxonomyPolicy policy;

    public TaxonomyCommandService(TaxonomyRepository repository, TaxonomyQueryService queries, TaxonomyPolicy policy) {
        this.repository = repository;
        this.queries = queries;
        this.policy = policy;
    }

    @Transactional
    public TaxonomyTermView create(TaxonomyRequest request) {
        String dimension = policy.requireDimension(request.dimension());
        Long parentId = request.parentId();
        if (parentId != null) policy.validateParent(dimension, queries.require(parentId));
        String slug = NumericSlugGenerator.forCreate(request.slug(),
                candidate -> repository.slugExists(candidate, null));
        policy.assertSlugFree(repository.slugExists(slug, null));
        Integer order = request.sortOrder() == null
                ? repository.nextOrder(dimension, parentId) : request.sortOrder();
        policy.validateSortOrder(order);

        EnglishTaxonomyTerm term = new EnglishTaxonomyTerm();
        term.setDimension(dimension);
        term.setParentId(parentId);
        term.setName(request.name().trim());
        term.setSlug(slug);
        term.setDescription(clean(request.description()));
        term.setSortOrder(order);
        term.setEnabled(request.enabled() == null || request.enabled());
        try {
            repository.insert(term);
        } catch (DuplicateKeyException ex) {
            throw policy.slugConflict();
        }
        return queries.view(queries.require(term.getId()));
    }

    @Transactional
    public TaxonomyTermView update(Long id, TaxonomyRequest request) {
        EnglishTaxonomyTerm term = queries.require(id);
        String dimension = policy.requireDimension(request.dimension());
        boolean hasChildren = repository.countChildren(id) > 0;
        policy.validateDimensionChange(term, dimension, hasChildren);
        Long parentId = request.parentId();
        policy.validateParentAssignment(id, parentId, hasChildren);
        if (parentId != null) policy.validateParent(dimension, queries.require(parentId));
        String slug = NumericSlugGenerator.forUpdate(request.slug(), term.getSlug());
        policy.assertSlugFree(repository.slugExists(slug, id));
        policy.validateSortOrder(request.sortOrder());

        term.setDimension(dimension);
        term.setParentId(parentId);
        term.setName(request.name().trim());
        term.setSlug(slug);
        term.setDescription(clean(request.description()));
        if (request.sortOrder() != null) term.setSortOrder(request.sortOrder());
        if (request.enabled() != null) term.setEnabled(request.enabled());
        try {
            repository.update(term);
        } catch (DuplicateKeyException ex) {
            throw policy.slugConflict();
        }
        return queries.view(queries.require(id));
    }

    @Transactional
    public void move(Long id, TaxonomyMoveRequest request) {
        EnglishTaxonomyTerm term = queries.require(id);
        List<Long> ids = repository.siblingIds(term.getDimension(), term.getParentId());
        ids.remove(id);
        int target = request.targetIndex() == null ? 0 : request.targetIndex();
        ids.add(Math.min(Math.max(target, 0), ids.size()), id);
        repository.normalizeOrder(ids);
    }

    @Transactional
    public void delete(Long id) {
        queries.require(id);
        boolean hasChildren = repository.countChildren(id) > 0;
        policy.assertDeletable(hasChildren, !hasChildren && repository.referencedByContent(id));
        repository.delete(id);
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
