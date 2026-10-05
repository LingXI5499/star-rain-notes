package com.starrainnotes.english.listening.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.listening.dto.ListeningItemDto.Item;
import com.starrainnotes.english.listening.dto.ListeningItemDto.Page;
import com.starrainnotes.english.listening.service.ListeningItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/english/content/listening")
public class ListeningPublicController {
    private final ListeningItemService service;

    @GetMapping
    public ApiResponse<Page> list(@RequestParam(required = false) String search,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(false, search, page, size));
    }

    @GetMapping("/{slug}")
    public ApiResponse<Item> get(@PathVariable String slug) {
        return ApiResponse.ok(service.get(slug, false));
    }
}
