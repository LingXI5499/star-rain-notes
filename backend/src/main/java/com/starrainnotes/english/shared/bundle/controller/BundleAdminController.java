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
    private final com.starrainnotes.english.shared.bundle.service.LearningBundleItemService items;

    public BundleAdminController(LearningBundleService service, com.starrainnotes.english.shared.bundle.service.LearningBundleItemService items) {
        this.service = service;
        this.items = items;
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

    @GetMapping("/{id}/items")
    public java.util.List<com.starrainnotes.english.shared.bundle.dto.BundleItemView> items(@PathVariable Long id) {
        return items.list(id, false);
    }

    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public com.starrainnotes.english.shared.bundle.dto.BundleItemView addItem(
            @PathVariable Long id, @Valid @RequestBody com.starrainnotes.english.shared.bundle.dto.BundleItemRequest request) {
        return items.add(id, request.contentType(), request.contentId());
    }

    @PostMapping("/{id}/items/{contentType}/{contentId}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveItem(@PathVariable Long id, @PathVariable String contentType, @PathVariable Long contentId,
                         @Valid @RequestBody com.starrainnotes.english.shared.bundle.dto.BundleItemMoveRequest request) {
        items.move(id, contentType, contentId, request.targetIndex());
    }

    @DeleteMapping("/{id}/items/{contentType}/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable Long id, @PathVariable String contentType, @PathVariable Long contentId) {
        items.remove(id, contentType, contentId);
    }
}
