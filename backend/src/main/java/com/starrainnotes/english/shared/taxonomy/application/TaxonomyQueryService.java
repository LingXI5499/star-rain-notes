package com.starrainnotes.english.shared.taxonomy.application;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.shared.taxonomy.dto.TaxonomyTermView;
import com.starrainnotes.english.shared.taxonomy.entity.EnglishTaxonomyTerm;
import com.starrainnotes.english.shared.taxonomy.api.TaxonomyTermReferencePort;
import com.starrainnotes.english.shared.taxonomy.infrastructure.TaxonomyRepository;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class TaxonomyQueryService {
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final TaxonomyRepository repository;
    private final List<TaxonomyTermReferencePort> references;
    private final SiteSettingsTimezone timezone;

    public TaxonomyQueryService(TaxonomyRepository repository, List<TaxonomyTermReferencePort> references,
                                SiteSettingsTimezone timezone) {
        this.repository = repository;
        this.references = references;
        this.timezone = timezone;
    }

    public List<TaxonomyTermView> tree() {
        List<EnglishTaxonomyTerm> all = repository.all();
        Map<Long, TaxonomyTermView> views = new LinkedHashMap<>();
        for (EnglishTaxonomyTerm term : all) views.put(term.getId(), toView(term, List.of()));
        for (EnglishTaxonomyTerm term : all) {
            if (term.getParentId() == null) continue;
            TaxonomyTermView parent = views.get(term.getParentId());
            if (parent == null) continue;
            List<TaxonomyTermView> children = new ArrayList<>(parent.children());
            children.add(views.get(term.getId()));
            views.put(term.getParentId(), new TaxonomyTermView(parent.id(), parent.parentId(),
                    parent.dimension(), parent.name(), parent.slug(), parent.description(),
                    parent.sortOrder(), parent.enabled(), parent.updatedAt(), children));
        }
        List<TaxonomyTermView> roots = new ArrayList<>();
        for (EnglishTaxonomyTerm term : all) {
            if (term.getParentId() == null) roots.add(views.get(term.getId()));
        }
        return roots;
    }

    public List<TaxonomyTermView> flat() {
        return repository.all().stream().map(term -> toView(term, null)).toList();
    }

    public EnglishTaxonomyTerm require(Long id) {
        EnglishTaxonomyTerm term = repository.find(id);
        if (term == null) throw notFound();
        return term;
    }

    public EnglishTaxonomyTerm requireBySlug(String slug) {
        EnglishTaxonomyTerm term = repository.findBySlug(slug);
        if (term == null) throw notFound();
        return term;
    }

    public EnglishTaxonomyTerm requireEnabled(Long id) {
        EnglishTaxonomyTerm term = require(id);
        if (!Boolean.TRUE.equals(term.getEnabled())) throw notFound();
        return term;
    }

    public List<EnglishTaxonomyTerm> descendants(Long id) {
        require(id);
        return repository.all().stream()
                .filter(term -> Objects.equals(term.getParentId(), id)).toList();
    }

    public int depthOf(Long id) {
        return require(id).getParentId() == null ? 0 : 1;
    }

    public List<Long> ancestorIds(Long id) {
        Long parentId = require(id).getParentId();
        return parentId == null ? List.of() : List.of(parentId);
    }

    public long usageCount(Long id) {
        require(id);
        long total = 0;
        for (TaxonomyTermReferencePort port : references) total += port.countReferences(id);
        return total;
    }

    public TaxonomyTermView view(EnglishTaxonomyTerm term) {
        return toView(term, null);
    }

    private TaxonomyTermView toView(EnglishTaxonomyTerm term, List<TaxonomyTermView> children) {
        return new TaxonomyTermView(term.getId(), term.getParentId(), term.getDimension(), term.getName(),
                term.getSlug(), term.getDescription(), term.getSortOrder(), term.getEnabled(),
                format(term.getUpdatedAt()), children);
    }

    private String format(LocalDateTime timestamp) {
        return timestamp == null ? null : timezone.atSite(timestamp).format(ISO_OFFSET);
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_TAXONOMY_NOT_FOUND",
                "Taxonomy term not found", "The taxonomy term does not exist.");
    }
}
