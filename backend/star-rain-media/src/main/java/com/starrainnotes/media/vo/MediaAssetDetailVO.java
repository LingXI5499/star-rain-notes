/*
 * MediaAssetDetailVO：媒体模块的响应数据文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 * 主要协作类型：MediaAssetEntity。
 * 阅读入口：from（从输入构造媒体资产详情）。
 */

package com.starrainnotes.media.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.starrainnotes.media.entity.MediaAssetEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 媒体资产详情视图：在列表字段基础上补充 referenceCount。
 *
 * MED-007 需要回答“这个媒体正在被谁引用”：这里只给数量，
 * 引用明细由 GET /api/admin/media/assets/{id}/references 提供。
 */
/**
 * 媒体资产详情响应对象，属于媒体模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetDetailVO {

    /**
     * 当前业务对象的数据库标识。
     * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /**
     * 原始名称。
     */
    private String originalName;
    /**
     * 媒体类型。
     */
    private String mediaType;
    /**
     * 文件的 MIME 类型。
     */
    private String mimeType;
    /**
     * 文件扩展名。
     */
    private String fileExtension;
    /**
     * 文件大小，单位为字节。
     */
    private Long sizeBytes;
    /**
     * 文件内容的 SHA-256 摘要。
     */
    private String sha256;
    /**
     * 宽度。
     */
    private Integer width;
    /**
     * 高度。
     */
    private Integer height;
    /**
     * 媒体访问级别。
     */
    private String accessLevel;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 存储提供方。
     */
    private String storageProvider;
    /**
     * 上传文件的账号标识，来自认证上下文。
     * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long uploadedByAccountId;
    /**
     * 已归档时间。
     */
    private LocalDateTime archivedAt;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
    /**
     * 内容地址。
     */
    private String contentUrl;
    /**
     * 引用数量。
     */
    private long referenceCount;

    /**
     * 从输入构造媒体资产详情。
     *
     * @param entity 数据库映射实体；类型为 <code>MediaAssetEntity</code>
     * @param referenceCount 引用数量；类型为 <code>long</code>
     * @return 由输入对象组装的媒体资产详情响应对象
     */
    public static MediaAssetDetailVO from(MediaAssetEntity entity, long referenceCount) {
        return MediaAssetDetailVO.builder()
                .id(entity.getId())
                .originalName(entity.getOriginalName())
                .mediaType(entity.getMediaType())
                .mimeType(entity.getMimeType())
                .fileExtension(entity.getFileExtension())
                .sizeBytes(entity.getSizeBytes())
                .sha256(entity.getSha256())
                .width(entity.getWidth())
                .height(entity.getHeight())
                .accessLevel(entity.getAccessLevel())
                .status(entity.getStatus())
                .storageProvider(entity.getStorageProvider())
                .uploadedByAccountId(entity.getUploadedByAccountId())
                .archivedAt(entity.getArchivedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .contentUrl(MediaAssetVO.contentUrl(entity.getId()))
                .referenceCount(referenceCount)
                .build();
    }
}
