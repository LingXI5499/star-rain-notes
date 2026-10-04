package com.starrainnotes.portfolio.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.constant.PortfolioPermissions;
import com.starrainnotes.portfolio.dto.IdOrderDTO;
import com.starrainnotes.portfolio.dto.WorkBodyDTO;
import com.starrainnotes.portfolio.dto.WorkCreateDTO;
import com.starrainnotes.portfolio.dto.WorkLinkDTO;
import com.starrainnotes.portfolio.dto.WorkMediaDTO;
import com.starrainnotes.portfolio.dto.WorkPatchDTO;
import com.starrainnotes.portfolio.enumeration.WorkType;
import com.starrainnotes.portfolio.service.PortfolioWorkService;
import com.starrainnotes.portfolio.vo.WorkVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/portfolio")
public class PortfolioAdminController {
    private final PortfolioWorkService works;

    @GetMapping("/works")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.READ_ADMIN + "')")
    public ApiResponse<PageResult<WorkVO>> list(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int pageSize,
                                                 @RequestParam(required = false) WorkType type,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String q) {
        return ApiResponse.ok(works.adminWorks(page, pageSize, type, status, q));
    }

    @PostMapping("/works")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> create(@Valid @RequestBody WorkCreateDTO request) {
        return ApiResponse.ok(works.create(request));
    }

    @GetMapping("/works/{id}")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.READ_ADMIN + "')")
    public ApiResponse<WorkVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(works.adminWork(id));
    }

    @GetMapping("/works/{id}/preview")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.READ_ADMIN + "')")
    public ApiResponse<WorkVO> preview(@PathVariable Long id) {
        return ApiResponse.ok(works.adminWork(id));
    }

    @PatchMapping("/works/{id}")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> update(@PathVariable Long id, @Valid @RequestBody WorkPatchDTO request) {
        return ApiResponse.ok(works.update(id, request));
    }

    @PutMapping("/works/{id}/body")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> body(@PathVariable Long id, @Valid @RequestBody WorkBodyDTO request) {
        return ApiResponse.ok(works.updateBody(id, request.getBodyMarkdown()));
    }

    @PutMapping("/works/{id}/detail")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> typeDetail(@PathVariable Long id, @RequestBody JsonNode request) {
        return ApiResponse.ok(works.updateDetail(id, request));
    }

    @PostMapping("/works/{id}/media")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> addMedia(@PathVariable Long id, @Valid @RequestBody WorkMediaDTO request) {
        return ApiResponse.ok(works.addMedia(id, request));
    }

    @PatchMapping("/media/{mediaId}")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> updateMedia(@PathVariable Long mediaId, @Valid @RequestBody WorkMediaDTO request) {
        return ApiResponse.ok(works.updateMedia(mediaId, request));
    }

    @DeleteMapping("/media/{mediaId}")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<Void> removeMedia(@PathVariable Long mediaId) {
        works.removeMedia(mediaId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/works/{id}/media/order")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> orderMedia(@PathVariable Long id, @Valid @RequestBody IdOrderDTO request) {
        return ApiResponse.ok(works.orderMedia(id, request));
    }

    @PostMapping("/works/{id}/links")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> addLink(@PathVariable Long id, @Valid @RequestBody WorkLinkDTO request) {
        return ApiResponse.ok(works.addLink(id, request));
    }

    @PatchMapping("/links/{linkId}")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> updateLink(@PathVariable Long linkId, @Valid @RequestBody WorkLinkDTO request) {
        return ApiResponse.ok(works.updateLink(linkId, request));
    }

    @DeleteMapping("/links/{linkId}")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<Void> removeLink(@PathVariable Long linkId) {
        works.removeLink(linkId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/works/{id}/links/order")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<WorkVO> orderLinks(@PathVariable Long id, @Valid @RequestBody IdOrderDTO request) {
        return ApiResponse.ok(works.orderLinks(id, request));
    }

    @PostMapping("/works/{id}/publish")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.PUBLISH + "')")
    public ApiResponse<WorkVO> publish(@PathVariable Long id) {
        return ApiResponse.ok(works.publish(id));
    }

    @PostMapping("/works/{id}/withdraw")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.WITHDRAW + "')")
    public ApiResponse<WorkVO> withdraw(@PathVariable Long id) {
        return ApiResponse.ok(works.withdraw(id));
    }

    @PostMapping("/works/{id}/restore")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.PUBLISH + "')")
    public ApiResponse<WorkVO> restore(@PathVariable Long id) {
        return ApiResponse.ok(works.restore(id));
    }

    @DeleteMapping("/works/{id}")
    @PreAuthorize("hasAuthority('" + PortfolioPermissions.EDIT + "')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        works.delete(id);
        return ApiResponse.ok(null);
    }
}
