/*
 * MediaAssetController：媒体模块的HTTP 接入文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、PageResult、MediaAccessLevel、MediaAccessLevelDTO、MediaQueryDTO、MediaAssetService、MediaAssetDetailVO。
 * 阅读入口：upload（上传媒体资产）；page（分页读取媒体资产）；detail（读取详情媒体资产）；changeAccessLevel（change访问Level处理入口）。
 */

package com.starrainnotes.media.controller;

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
/**
 * 媒体资产控制器，属于媒体模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequestMapping("/api/admin/media/assets")
public class MediaAssetController {

    /**
     * 上传业务服务。
     * 协作类型：<code>MediaUploadService</code>。
     */
    private final MediaUploadService uploadService;
    /**
     * 资产业务服务。
     * 协作类型：<code>MediaAssetService</code>。
     */
    private final MediaAssetService assetService;
    /**
     * 引用业务服务。
     * 协作类型：<code>MediaReferenceService</code>。
     */
    private final MediaReferenceService referenceService;

    /**
     * 构造MediaAssetController，接收本类使用的依赖或初始化参数。
     *
     * @param uploadService 上传业务服务；类型为 <code>MediaUploadService</code>
     * @param assetService 资产业务服务；类型为 <code>MediaAssetService</code>
     * @param referenceService 引用业务服务；类型为 <code>MediaReferenceService</code>
     */
    public MediaAssetController(MediaUploadService uploadService,
                               MediaAssetService assetService,
                               MediaReferenceService referenceService) {
        this.uploadService = uploadService;
        this.assetService = assetService;
        this.referenceService = referenceService;
    }

    // MED-001 上传媒体
    /**
     * 上传媒体资产。
     * HTTP 映射：<code>@PostMapping(consumes = "multipart/form-data")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'media:upload\')")</code>。
     * 主要协作调用：<code>uploadService.upload</code>。
     *
     * @param file 待处理的上传文件；类型为 <code>MultipartFile</code>
     * @param accessLevel 媒体访问级别；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;MediaAssetVO&gt;</code> 的处理结果
     */
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('media:upload')")
    public ApiResponse<MediaAssetVO> upload(@RequestPart("file") MultipartFile file,
                                            @RequestParam(required = false) String accessLevel) {
        MediaAccessLevel level = (accessLevel == null || accessLevel.isBlank())
                ? null
                : MediaAccessLevel.parse(accessLevel);
        return ApiResponse.ok(uploadService.upload(file, level));
    }

    // MED-002 查询媒体资产
    /**
     * 分页读取媒体资产。
     * HTTP 映射：<code>@GetMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'media:read\')")</code>。
     * 主要协作调用：<code>assetService.page</code>。
     *
     * @param query 查询参数；类型为 <code>MediaQueryDTO</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;MediaAssetVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping
    @PreAuthorize("hasAuthority('media:read')")
    public ApiResponse<PageResult<MediaAssetVO>> page(MediaQueryDTO query) {
        return ApiResponse.ok(assetService.page(query));
    }

    // 媒体详情（含引用数量）
    /**
     * 读取详情媒体资产。
     * HTTP 映射：<code>@GetMapping("/{mediaAssetId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'media:read\')")</code>。
     * 主要协作调用：<code>assetService.getDetail</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;MediaAssetDetailVO&gt;</code> 的处理结果
     */
    @GetMapping("/{mediaAssetId}")
    @PreAuthorize("hasAuthority('media:read')")
    public ApiResponse<MediaAssetDetailVO> detail(@PathVariable Long mediaAssetId) {
        return ApiResponse.ok(assetService.getDetail(mediaAssetId));
    }

    // 调整访问级别：不新增 Formal Use Case，属于 MED-006 生命周期管理
    /**
     * change访问Level处理入口。
     * HTTP 映射：<code>@PatchMapping("/{mediaAssetId}/access-level")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'media:access-manage\')")</code>。
     * 主要协作调用：<code>assetService.changeAccessLevel</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>MediaAccessLevelDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PatchMapping("/{mediaAssetId}/access-level")
    @PreAuthorize("hasAuthority('media:access-manage')")
    public ApiResponse<Void> changeAccessLevel(@PathVariable Long mediaAssetId,
                                               @Valid @RequestBody MediaAccessLevelDTO request) {
        assetService.changeAccessLevel(mediaAssetId, MediaAccessLevel.parse(request.getAccessLevel()));
        return ApiResponse.ok(null);
    }

    // MED-006 归档：存在业务引用时会被拒绝
    /**
     * archive处理入口。
     * HTTP 映射：<code>@PostMapping("/{mediaAssetId}/archive")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'media:archive\')")</code>。
     * 主要协作调用：<code>assetService.archive</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/{mediaAssetId}/archive")
    @PreAuthorize("hasAuthority('media:archive')")
    public ApiResponse<Void> archive(@PathVariable Long mediaAssetId) {
        assetService.archive(mediaAssetId);
        return ApiResponse.ok(null);
    }

    // 归档生命周期里的恢复动作，不新增 Formal Use Case
    /**
     * 恢复媒体资产。
     * HTTP 映射：<code>@PostMapping("/{mediaAssetId}/restore")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'media:restore\')")</code>。
     * 主要协作调用：<code>assetService.restore</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/{mediaAssetId}/restore")
    @PreAuthorize("hasAuthority('media:restore')")
    public ApiResponse<Void> restore(@PathVariable Long mediaAssetId) {
        assetService.restore(mediaAssetId);
        return ApiResponse.ok(null);
    }

    // MED-007 查询媒体引用情况
    /**
     * references处理入口。
     * HTTP 映射：<code>@GetMapping("/{mediaAssetId}/references")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'media:reference-read\')")</code>。
     * 主要协作调用：<code>referenceService.listByAsset</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;MediaReferenceVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/{mediaAssetId}/references")
    @PreAuthorize("hasAuthority('media:reference-read')")
    public ApiResponse<List<MediaReferenceVO>> references(@PathVariable Long mediaAssetId) {
        return ApiResponse.ok(referenceService.listByAsset(mediaAssetId));
    }
}
