package com.starrainnotes.account.review.controller;

import com.starrainnotes.account.review.dto.ContentReviewView;
import com.starrainnotes.account.review.dto.ReviewDecisionRequest;
import com.starrainnotes.account.review.service.ContentReviewService;
import com.starrainnotes.account.security.AccountPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/super-admin/content-reviews")
public class ContentReviewController {
    private final ContentReviewService service;

    public ContentReviewController(ContentReviewService service) {
        this.service = service;
    }

    @GetMapping
    public List<ContentReviewView> list(@RequestParam(required = false) String status,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int pageSize) {
        return service.list(status, page, pageSize);
    }

    @PostMapping("/{id}/approve")
    public ContentReviewView approve(@PathVariable Long id, @Valid @RequestBody(required = false) ReviewDecisionRequest request,
                                     Authentication authentication) {
        return service.approve(id, actor(authentication), request == null ? null : request.note());
    }

    @PostMapping("/{id}/reject")
    public ContentReviewView reject(@PathVariable Long id, @Valid @RequestBody(required = false) ReviewDecisionRequest request,
                                    Authentication authentication) {
        return service.reject(id, actor(authentication), request == null ? null : request.note());
    }

    private Long actor(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal
                ? principal.getId() : null;
    }
}
