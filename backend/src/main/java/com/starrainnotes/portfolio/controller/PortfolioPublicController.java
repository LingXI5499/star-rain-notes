package com.starrainnotes.portfolio.controller;

import com.starrainnotes.portfolio.dto.PublicProjectDetailView;
import com.starrainnotes.portfolio.dto.PublicProjectSummaryView;
import com.starrainnotes.portfolio.service.PortfolioQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public portfolio endpoints (04 §12). Published only; Draft/Withdrawn → 404.
 */
@RestController
@RequestMapping("/api/v1/public/portfolio/projects")
@RequiredArgsConstructor
public class PortfolioPublicController {

    private final PortfolioQueryService queryService;

    @GetMapping
    public List<PublicProjectSummaryView> list() {
        return queryService.publicList();
    }

    @GetMapping("/{slug}")
    public PublicProjectDetailView detail(@PathVariable String slug) {
        return queryService.publicDetail(slug);
    }
}
