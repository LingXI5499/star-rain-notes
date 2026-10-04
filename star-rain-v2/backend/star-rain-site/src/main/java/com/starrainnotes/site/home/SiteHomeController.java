package com.starrainnotes.site.home;

import com.starrainnotes.common.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/site")
public class SiteHomeController {
    private final HomeAggregationService service;

    @GetMapping("/home")
    public ApiResponse<SiteHomeView> home() { return ApiResponse.ok(service.home()); }
}
