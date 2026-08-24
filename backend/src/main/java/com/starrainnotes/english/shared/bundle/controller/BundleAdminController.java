package com.starrainnotes.english.shared.bundle.controller;

import com.starrainnotes.english.shared.bundle.dto.BundleRequest;
import com.starrainnotes.english.shared.bundle.dto.BundleView;
import com.starrainnotes.english.shared.bundle.service.LearningBundleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin learning-bundle management (方案 §10.6). Writes use the frozen session
 * + CSRF flow.
 */
@RestController
@RequestMapping("/api/v1/admin/english/bundles")
public class BundleAdminController {

    private final LearningBundleService service;

    public BundleAdminController(LearningBundleService service) {
        this.service = service;
    }

    @GetMapping
    public List<BundleView> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BundleView create(@Valid @RequestBody BundleRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public BundleView get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public BundleView update(@PathVariable Long id, @Valid @RequestBody BundleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/publish")
    public BundleView publish(@PathVariable Long id) {
        return service.publish(id);
    }

    @PostMapping("/{id}/withdraw")
    public BundleView withdraw(@PathVariable Long id) {
        return service.withdraw(id);
    }
}
