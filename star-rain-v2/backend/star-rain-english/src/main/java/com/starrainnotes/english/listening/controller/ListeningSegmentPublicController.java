package com.starrainnotes.english.listening.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.listening.dto.ListeningSegmentDto;
import com.starrainnotes.english.listening.service.ListeningSegmentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/english/listening")
public class ListeningSegmentPublicController {
    private final ListeningSegmentService service;

    @GetMapping("/{slug}/segments")
    public ApiResponse<List<ListeningSegmentDto.Segment>> list(@PathVariable String slug) {
        return ApiResponse.ok(service.list(slug, false));
    }
}
