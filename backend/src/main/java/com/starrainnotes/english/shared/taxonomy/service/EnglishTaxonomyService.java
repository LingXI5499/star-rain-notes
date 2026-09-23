package com.starrainnotes.english.shared.taxonomy.service;

import com.starrainnotes.english.shared.taxonomy.application.TaxonomyCommandService;
import com.starrainnotes.english.shared.taxonomy.application.TaxonomyQueryService;
import com.starrainnotes.english.shared.taxonomy.domain.TaxonomyPolicy;
import com.starrainnotes.english.shared.taxonomy.dto.TaxonomyMoveRequest;
import com.starrainnotes.english.shared.taxonomy.dto.TaxonomyRequest;
import com.starrainnotes.english.shared.taxonomy.dto.TaxonomyTermView;
import com.starrainnotes.english.shared.taxonomy.entity.EnglishTaxonomyTerm;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/** Compatibility facade for existing taxonomy controllers. */
@Service
public class EnglishTaxonomyService {
    public static final Set<String> DIMENSIONS = TaxonomyPolicy.DIMENSIONS;
    private final TaxonomyQueryService queries;
    private final TaxonomyCommandService commands;

    public EnglishTaxonomyService(TaxonomyQueryService queries, TaxonomyCommandService commands) {
        this.queries = queries;
        this.commands = commands;
    }

    public List<TaxonomyTermView> tree() { return queries.tree(); }
    public List<TaxonomyTermView> flat() { return queries.flat(); }
    public TaxonomyTermView create(TaxonomyRequest request) { return commands.create(request); }
    public TaxonomyTermView update(Long id, TaxonomyRequest request) { return commands.update(id, request); }
    public void move(Long id, TaxonomyMoveRequest request) { commands.move(id, request); }
    public void delete(Long id) { commands.delete(id); }
    public EnglishTaxonomyTerm require(Long id) { return queries.require(id); }
    public EnglishTaxonomyTerm requireBySlug(String slug) { return queries.requireBySlug(slug); }
}
