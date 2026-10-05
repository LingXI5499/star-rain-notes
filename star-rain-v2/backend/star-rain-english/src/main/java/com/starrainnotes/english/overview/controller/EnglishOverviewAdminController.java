package com.starrainnotes.english.overview.controller;

import com.starrainnotes.english.overview.vo.EnglishOverviewVO;

import com.starrainnotes.english.overview.service.EnglishOverviewService;

import com.starrainnotes.english.overview.dto.EnglishOverviewRequestDTO;

import com.starrainnotes.common.result.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/english")
@PreAuthorize("hasAuthority('english:overview-manage')")
public class EnglishOverviewAdminController {
    private final EnglishOverviewService service;

    @GetMapping("/overview")
    public ApiResponse<EnglishOverviewVO> get() { return ApiResponse.ok(service.get()); }

    @PutMapping("/overview")
    public ApiResponse<EnglishOverviewVO> update(@Valid @RequestBody EnglishOverviewRequestDTO request) {
        return ApiResponse.ok(service.update(request));
    }
}
