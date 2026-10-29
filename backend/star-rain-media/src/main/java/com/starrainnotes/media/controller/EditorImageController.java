package com.starrainnotes.media.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.media.service.EditorImageService;
import com.starrainnotes.media.vo.MediaAssetVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/media/editor-images")
@PreAuthorize("hasAuthority('media:upload')")
public class EditorImageController {
    private final EditorImageService service;

    public record ImportRequest(@NotBlank @Size(max = 2048) String url) { }

    @PostMapping("/import")
    public ApiResponse<MediaAssetVO> importUrl(@Valid @RequestBody ImportRequest request) {
        return ApiResponse.ok(service.importUrl(request.url()));
    }

    @PostMapping("/{id}/prepare")
    public ApiResponse<MediaAssetVO> prepare(@PathVariable Long id) {
        return ApiResponse.ok(service.prepare(id));
    }
}
