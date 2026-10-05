package com.starrainnotes.site.controller;
import com.starrainnotes.site.api.vo.SitePublicConfigVO;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.site.api.SitePublicApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/site")
public class SiteConfigPublicController {
    private final SitePublicApi site;

    @GetMapping("/config")
    public ApiResponse<SitePublicConfigVO> config() { return ApiResponse.ok(site.config()); }
}
