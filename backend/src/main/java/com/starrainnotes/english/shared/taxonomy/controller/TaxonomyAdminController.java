package com.starrainnotes.english.shared.taxonomy.controller;

import com.starrainnotes.english.shared.taxonomy.dto.TaxonomyMoveRequest;
import com.starrainnotes.english.shared.taxonomy.dto.TaxonomyRequest;
import com.starrainnotes.english.shared.taxonomy.dto.TaxonomyTermView;
import com.starrainnotes.english.shared.taxonomy.service.EnglishTaxonomyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin taxonomy management (方案 §10.2). Writes require the frozen session +
 * CSRF flow enforced by the security config.
 */
@RestController
@RequestMapping("/api/v1/admin/english/taxonomy")
public class TaxonomyAdminController {

    private final EnglishTaxonomyService service;

    public TaxonomyAdminController(EnglishTaxonomyService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaxonomyTermView> list(@RequestParam(defaultValue = "tree") String view) {
        return "flat".equalsIgnoreCase(view) ? service.flat() : service.tree();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaxonomyTermView create(@Valid @RequestBody TaxonomyRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public TaxonomyTermView update(@PathVariable Long id, @Valid @RequestBody TaxonomyRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void move(@PathVariable Long id, @RequestBody TaxonomyMoveRequest request) {
        service.move(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
