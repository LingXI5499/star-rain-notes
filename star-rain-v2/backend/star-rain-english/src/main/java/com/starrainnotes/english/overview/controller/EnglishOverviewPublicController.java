package com.starrainnotes.english.overview.controller;

import com.starrainnotes.english.overview.dto.EnglishOverviewView;

import com.starrainnotes.english.overview.service.EnglishOverviewService;

import com.starrainnotes.common.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/english")
public class EnglishOverviewPublicController {
    private final EnglishOverviewService service;

    @GetMapping("/overview")
    public ApiResponse<EnglishOverviewView> get() { return ApiResponse.ok(service.get()); }
}
