package com.starrainnotes.profile.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.profile.service.ProfileService;
import com.starrainnotes.profile.api.vo.ProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/profile")
public class ProfilePublicController {
    private final ProfileService service;

    @GetMapping
    public ApiResponse<ProfileVO> get() {
        return ApiResponse.ok(service.publicProfile());
    }
}
