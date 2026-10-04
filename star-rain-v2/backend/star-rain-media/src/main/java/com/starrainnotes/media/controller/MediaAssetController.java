package com.starrainnotes.media.controller;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.media.enumeration.MediaAccessLevel;
import com.starrainnotes.media.dto.MediaAccessLevelDTO;
import com.starrainnotes.media.dto.MediaQueryDTO;
import com.starrainnotes.media.service.MediaAssetService;
import com.starrainnotes.media.vo.MediaAssetDetailVO;
import com.starrainnotes.media.vo.MediaAssetVO;
import com.starrainnotes.media.service.MediaReferenceService;
import com.starrainnotes.media.vo.MediaReferenceVO;
import com.starrainnotes.media.service.MediaUploadService;
import com.starrainnotes.media.upload.MediaUploadLimiter;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/*
 * 后台媒体库接口。
 *
 * URL 层只要求“已认证”（见 MediaSecurityContributor），
 * 具体权限由这里的 @PreAuthorize 逐个端点声明。
 * MED-003 / MED-004 不在这里开放：浏览器无法直接创建跨模块引用。
 */
@RestController
@RequestMapping("/api/admin/media/assets")
public class MediaAssetController {

    private final MediaUploadService uploadService;
    private final MediaAssetService assetService;
    private final MediaReferenceService referenceService;
    private final CurrentActorApi currentActor;
    private final MediaUploadLimiter uploadLimiter;

    public MediaAssetController(MediaUploadService uploadService,
                               MediaAssetService assetService,
                               MediaReferenceService referenceService,
                               CurrentActorApi currentActor,
                               MediaUploadLimiter uploadLimiter) {
        this.uploadService = uploadService;
        this.assetService = assetService;
        this.referenceService = referenceService;
        this.currentActor = currentActor;
        this.uploadLimiter = uploadLimiter;
    }

    // MED-001 上传媒体
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('media:upload')")
    public ApiResponse<MediaAssetVO> upload(@RequestPart("file") MultipartFile file,
                                            @RequestParam(required = false) String accessLevel) {
        MediaAccessLevel level = (accessLevel == null || accessLevel.isBlank())
                ? null
                : MediaAccessLevel.parse(accessLevel);
        uploadLimiter.check(currentActor.current().getAccountId());
        return ApiResponse.ok(uploadService.upload(file, level));
    }

    // MED-002 查询媒体资产
    @GetMapping
    @PreAuthorize("hasAuthority('media:read')")
    public ApiResponse<PageResult<MediaAssetVO>> page(MediaQueryDTO query) {
        return ApiResponse.ok(assetService.page(query));
    }

    // 媒体详情（含引用数量）
    @GetMapping("/{mediaAssetId}")
    @PreAuthorize("hasAuthority('media:read')")
    public ApiResponse<MediaAssetDetailVO> detail(@PathVariable Long mediaAssetId) {
        return ApiResponse.ok(assetService.getDetail(mediaAssetId));
    }

    // 调整访问级别：不新增 Formal Use Case，属于 MED-006 生命周期管理
    @PatchMapping("/{mediaAssetId}/access-level")
    @PreAuthorize("hasAuthority('media:access-manage')")
    public ApiResponse<Void> changeAccessLevel(@PathVariable Long mediaAssetId,
                                               @Valid @RequestBody MediaAccessLevelDTO request) {
        assetService.changeAccessLevel(mediaAssetId, MediaAccessLevel.parse(request.getAccessLevel()));
        return ApiResponse.ok(null);
    }

    // MED-006 归档：存在业务引用时会被拒绝
    @PostMapping("/{mediaAssetId}/archive")
    @PreAuthorize("hasAuthority('media:archive')")
    public ApiResponse<Void> archive(@PathVariable Long mediaAssetId) {
        assetService.archive(mediaAssetId);
        return ApiResponse.ok(null);
    }

    // 归档生命周期里的恢复动作，不新增 Formal Use Case
    @PostMapping("/{mediaAssetId}/restore")
    @PreAuthorize("hasAuthority('media:restore')")
    public ApiResponse<Void> restore(@PathVariable Long mediaAssetId) {
        assetService.restore(mediaAssetId);
        return ApiResponse.ok(null);
    }

    // MED-007 查询媒体引用情况
    @GetMapping("/{mediaAssetId}/references")
    @PreAuthorize("hasAuthority('media:reference-read')")
    public ApiResponse<List<MediaReferenceVO>> references(@PathVariable Long mediaAssetId) {
        return ApiResponse.ok(referenceService.listByAsset(mediaAssetId));
    }
}
