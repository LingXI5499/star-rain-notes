package com.starrainnotes.media.controller;

import com.starrainnotes.media.service.MediaAccessService;
import com.starrainnotes.media.vo.MediaContentVO;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/*
 * MED-005 资源读取端点。
 *
 * 返回二进制内容，因此这里不使用 ApiResponse 包装；错误仍由
 * GlobalApiExceptionHandler 统一输出 ApiResponse 形状的 JSON。
 *
 * Content-Length 显式设置，避免 InputStreamResource 为了计算长度而读完整条流。
 *
 * Range 支持：音频/视频需要拖动进度，浏览器会发 Range 请求。
 *   无 Range        → 200 + 完整内容
 *   合法单区间      → 206 + Content-Range
 *   区间不可满足    → 416（由 MediaAccessService 抛出，带 Content-Range 语义的错误码）
 */
@RestController
public class MediaContentController {

    private static final String PUBLIC_CACHE = "public, max-age=86400";

    // 受保护内容不进共享缓存，也不进浏览器持久缓存
    private static final String PROTECTED_CACHE = "private, no-store";

    private final MediaAccessService accessService;

    public MediaContentController(MediaAccessService accessService) {
        this.accessService = accessService;
    }

    @GetMapping("/api/media/assets/{mediaAssetId}/content")
    public ResponseEntity<InputStreamResource> content(
            @PathVariable Long mediaAssetId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        MediaContentVO content = accessService.open(mediaAssetId, rangeHeader);

        HttpHeaders headers = new HttpHeaders();
        // MIME 只取自入库时的白名单规范值，不信任浏览器声明
        headers.setContentType(MediaType.parseMediaType(content.getMimeType()));
        headers.setContentLength(content.contentLength());
        headers.setCacheControl("PUBLIC".equals(content.getAccessLevel()) ? PUBLIC_CACHE : PROTECTED_CACHE);

        // 声明支持区间请求，音频/视频才能拖动进度与断点续传
        headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");

        // 防止浏览器把内容嗅探成别的类型
        headers.add("X-Content-Type-Options", "nosniff");

        // 用内容摘要做 ETag，媒体一旦落库内容就不可变
        headers.setETag("\"" + content.getSha256() + "\"");

        /*
         * 图片/音频/视频 inline 展示；文档与压缩包按附件下载。
         * 文件名按 UTF-8 编码，避免中文名乱码。
         */
        ContentDisposition disposition = (content.inlineDisplay()
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(content.getOriginalName(), StandardCharsets.UTF_8)
                .build();
        headers.setContentDisposition(disposition);

        if (content.partial()) {
            headers.add(HttpHeaders.CONTENT_RANGE, content.getRange().contentRange(content.getTotalSize()));
        }

        HttpStatus status = content.partial() ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;
        return ResponseEntity.status(status)
                .headers(headers)
                .body(new InputStreamResource(content.getStream()));
    }
}
