package com.starrainnotes.search.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.search.exception.SearchTypeInvalidException;
import com.starrainnotes.search.index.SearchIndexService;
import com.starrainnotes.search.mapper.SearchDocumentMapper;
import com.starrainnotes.search.rebuild.SearchRebuildService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/search")
@PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('search:rebuild')")
public class SearchRebuildController {
    private final SearchRebuildService service;
    private final SearchDocumentMapper mapper;

    @PostMapping("/rebuild")
    public ApiResponse<Map<String, Object>> rebuild(@RequestParam(required = false) String type) {
        if (type == null || type.isBlank()) service.rebuildAll();
        else {
            if (!SearchIndexService.TYPES.contains(type)) {
                throw new SearchTypeInvalidException();
            }
            service.rebuildType(type);
        }
        return ApiResponse.ok(Map.of("activeDocuments", mapper.activeCount()));
    }
}
