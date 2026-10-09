package com.starrainnotes.site.controller;

import com.starrainnotes.site.dto.SiteConfigPatchDTO;
import com.starrainnotes.site.dto.SiteMediaRequestDTO;
import com.starrainnotes.site.service.SiteConfigService;
import com.starrainnotes.site.api.vo.SitePublicConfigVO;

import com.starrainnotes.common.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/site")
@PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('site:config-manage')")
public class SiteConfigAdminController {
    private final SiteConfigService service;

    @PatchMapping("/config")
    public ApiResponse<SitePublicConfigVO> patch(@RequestBody SiteConfigPatchDTO request) {
        return ApiResponse.ok(service.patch(request));
    }

    @PutMapping("/{kind:logo|favicon}")
    public ApiResponse<SitePublicConfigVO> setMedia(@PathVariable String kind, @RequestBody SiteMediaRequestDTO request) {
        return ApiResponse.ok(service.setMediaRequired(kind, request));
    }

    @DeleteMapping("/{kind:logo|favicon}")
    public ApiResponse<SitePublicConfigVO> clearMedia(@PathVariable String kind) {
        return ApiResponse.ok(service.setMedia(kind, null));
    }
}
