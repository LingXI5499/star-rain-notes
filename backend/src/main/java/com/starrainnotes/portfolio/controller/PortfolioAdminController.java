package com.starrainnotes.portfolio.controller;

import com.starrainnotes.portfolio.dto.AdminProjectDetailView;
import com.starrainnotes.portfolio.dto.AdminProjectPageView;
import com.starrainnotes.portfolio.dto.CreateProjectRequest;
import com.starrainnotes.portfolio.dto.UpdateProjectRequest;
import com.starrainnotes.portfolio.service.PortfolioService;
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

/**
 * Admin portfolio project management (04 §12). Lifecycle only via /publish
 * and /withdraw actions.
 */
@RestController
@RequestMapping("/api/v1/admin/portfolio/projects")
public class PortfolioAdminController {

    private final PortfolioService portfolioService;

    public PortfolioAdminController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public AdminProjectPageView list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int pageSize,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String projectStatus,
                                     @RequestParam(required = false) String q) {
        return portfolioService.adminList(page, pageSize, status, projectStatus, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProjectDetailView create(@Valid @RequestBody CreateProjectRequest request) {
        return portfolioService.create(request);
    }

    @GetMapping("/{projectId}")
    public AdminProjectDetailView detail(@PathVariable Long projectId) {
        return portfolioService.adminDetail(projectId);
    }

    @PutMapping("/{projectId}")
    public AdminProjectDetailView update(@PathVariable Long projectId,
                                         @Valid @RequestBody UpdateProjectRequest request) {
        return portfolioService.update(projectId, request);
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId) {
        portfolioService.delete(projectId);
    }

    @PostMapping("/{projectId}/publish")
    public AdminProjectDetailView publish(@PathVariable Long projectId) {
        return portfolioService.publish(projectId);
    }

    @PostMapping("/{projectId}/withdraw")
    public AdminProjectDetailView withdraw(@PathVariable Long projectId) {
        return portfolioService.withdraw(projectId);
    }
}
