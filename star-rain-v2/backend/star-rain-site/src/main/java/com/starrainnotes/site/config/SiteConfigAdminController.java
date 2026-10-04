package com.starrainnotes.site.config;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.site.exception.SiteConfigException;
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
        if (request == null || request.getMediaAssetId() == null || request.getMediaAssetId() <= 0)
            throw new SiteConfigException("SITE_MEDIA_INVALID", "请选择有效的站点图片", 400);
        return ApiResponse.ok(service.setMedia(kind, request.getMediaAssetId()));
    }

    @DeleteMapping("/{kind:logo|favicon}")
    public ApiResponse<SitePublicConfig> clearMedia(@PathVariable String kind) {
        return ApiResponse.ok(service.setMedia(kind, null));
    }
}
