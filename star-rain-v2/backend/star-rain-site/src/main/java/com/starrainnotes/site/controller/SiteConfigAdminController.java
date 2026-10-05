package com.starrainnotes.site.controller;

import com.starrainnotes.site.dto.SiteConfigPatch;
import com.starrainnotes.site.dto.SiteMediaRequest;
import com.starrainnotes.site.service.SiteConfigService;
import com.starrainnotes.site.api.dto.SitePublicConfig;

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
    public ApiResponse<SitePublicConfig> patch(@RequestBody SiteConfigPatch request) {
        return ApiResponse.ok(service.patch(request));
    }

    @PutMapping("/{kind:logo|favicon}")
    public ApiResponse<SitePublicConfig> setMedia(@PathVariable String kind, @RequestBody SiteMediaRequest request) {
        return ApiResponse.ok(service.setMediaRequired(kind, request));
    }

    @DeleteMapping("/{kind:logo|favicon}")
    public ApiResponse<SitePublicConfig> clearMedia(@PathVariable String kind) {
        return ApiResponse.ok(service.setMedia(kind, null));
    }
}
