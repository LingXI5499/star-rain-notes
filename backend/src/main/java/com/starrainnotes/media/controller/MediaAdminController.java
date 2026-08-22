package com.starrainnotes.media.controller;

import com.starrainnotes.media.dto.MediaAssetView;
import com.starrainnotes.media.dto.MediaPageView;
import com.starrainnotes.media.service.MediaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin media library (04 §16): browse/search/filter, upload and delete.
 * All endpoints require an authenticated admin session + CSRF for mutations.
 */
@RestController
@RequestMapping("/api/v1/admin/media-assets")
public class MediaAdminController {

    private final MediaService mediaService;

    public MediaAdminController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping
    public MediaPageView list(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int pageSize,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) String assetType) {
        return mediaService.list(page, pageSize, q, assetType);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MediaAssetView upload(@RequestParam("file") MultipartFile file) {
        return mediaService.upload(file);
    }

    @DeleteMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long mediaId) {
        mediaService.delete(mediaId);
    }
}
