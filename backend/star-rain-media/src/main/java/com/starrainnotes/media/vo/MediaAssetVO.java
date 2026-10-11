/*
 * MediaAssetVO：媒体模块的响应数据文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 * 主要协作类型：MediaAssetEntity。
 * 阅读入口：contentUrl（内容地址处理入口）；from（从输入构造媒体资产）。
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
 * 媒体资产列表 / 上传响应视图。
 *
 * 不暴露 storageKey：存储定位属于模块内部实现，业务模块只保存 mediaAssetId。
 * sameSha256Count 只在重复检测时有意义，>1 表示库里已有同内容素材，
 * 前端可据此提示，但系统不会静默合并成同一条 MediaAsset。
 */
/**
 * 媒体资产响应对象，属于媒体模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetVO {

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
     * 内容地址。
     */
    private String contentUrl;
    /**
     * same摘要256数量。
     */
    private long sameSha256Count;

    /**
     * 内容地址处理入口。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String contentUrl(Long id) {
        return id == null ? null : "/api/media/assets/" + id + "/content";
    }

    /**
     * 从输入构造媒体资产。
     * 主要协作调用：<code>contentUrl</code>。
     *
     * @param entity 数据库映射实体；类型为 <code>MediaAssetEntity</code>
     * @param sameSha256Count same摘要256数量；类型为 <code>long</code>
     * @return 由输入对象组装的媒体资产响应对象
     */
    public static MediaAssetVO from(MediaAssetEntity entity, long sameSha256Count) {
        return MediaAssetVO.builder()
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
                .contentUrl(contentUrl(entity.getId()))
                .sameSha256Count(sameSha256Count)
                .build();
    }
}
