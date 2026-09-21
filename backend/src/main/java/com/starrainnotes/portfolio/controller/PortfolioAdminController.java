package com.starrainnotes.portfolio.controller;

import com.starrainnotes.portfolio.dto.AdminProjectDetailView;
import com.starrainnotes.portfolio.dto.AdminProjectPageView;
import com.starrainnotes.portfolio.dto.CreateProjectRequest;
import com.starrainnotes.portfolio.dto.UpdateProjectRequest;
import com.starrainnotes.portfolio.service.PortfolioService;
import com.starrainnotes.portfolio.service.PortfolioPrototypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;

/**
 * Admin portfolio project management (04 §12). Lifecycle only via /publish
 * and /withdraw actions.
 */
@RestController
@RequestMapping("/api/v1/admin/portfolio/projects")
@RequiredArgsConstructor
public class PortfolioAdminController {

    private final PortfolioService portfolioService;
    private final PortfolioPrototypeService prototypeService;

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

    @PostMapping(value = "/{projectId}/prototype", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public com.starrainnotes.portfolio.dto.ProjectPrototypeView uploadPrototype(@PathVariable Long projectId, @RequestParam("file") MultipartFile file) {
        AdminProjectDetailView project = portfolioService.adminDetail(projectId);
        return prototypeService.upload(projectId, file, "PUBLISHED".equals(project.publishStatus()));
    }

    @PostMapping("/{projectId}/prototype/media/{mediaId}")
    public com.starrainnotes.portfolio.dto.ProjectPrototypeView attachPrototype(@PathVariable Long projectId,
                                                                                @PathVariable Long mediaId) {
        AdminProjectDetailView project = portfolioService.adminDetail(projectId);
        return prototypeService.attach(projectId, mediaId, "PUBLISHED".equals(project.publishStatus()));
    }

    @DeleteMapping("/{projectId}/prototype")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePrototype(@PathVariable Long projectId) { portfolioService.adminDetail(projectId); prototypeService.delete(projectId); }

    @GetMapping("/{projectId}/prototype-preview/{*path}")
    public ResponseEntity<Resource> previewPrototype(@PathVariable Long projectId, @PathVariable String path) {
        Resource file = prototypeService.previewResource(projectId, path);
        String name = file.getFilename() == null ? "" : file.getFilename();
        MediaType type = MediaTypeFactory.getMediaType(name).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff").body(file);
    }
}
