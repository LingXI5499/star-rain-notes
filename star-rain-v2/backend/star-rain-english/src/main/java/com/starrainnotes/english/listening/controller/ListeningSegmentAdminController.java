package com.starrainnotes.english.listening.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.listening.dto.ListeningSegmentDto;
import com.starrainnotes.english.listening.service.ListeningSegmentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/english/listening")
@PreAuthorize("hasAuthority('english:content-read-admin')")
public class ListeningSegmentAdminController {
    private final ListeningSegmentService service;

    @GetMapping("/{itemId}/segments")
    public ApiResponse<List<ListeningSegmentDto.Segment>> list(@PathVariable String itemId) {
        return ApiResponse.ok(service.list(itemId, true));
    }

    @PostMapping("/{itemId}/segments")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<ListeningSegmentDto.Segment> create(@PathVariable String itemId,
            @RequestBody ListeningSegmentDto.Request request) {
        return ApiResponse.ok(service.create(itemId, request));
    }

    @PutMapping("/{itemId}/segments/{segmentId}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<ListeningSegmentDto.Segment> update(@PathVariable String itemId,
            @PathVariable String segmentId, @RequestBody ListeningSegmentDto.Request request) {
        return ApiResponse.ok(service.update(itemId, segmentId, request));
    }

    @DeleteMapping("/{itemId}/segments/{segmentId}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Void> delete(@PathVariable String itemId, @PathVariable String segmentId) {
        service.delete(itemId, segmentId);
        return ApiResponse.ok(null);
    }
}
