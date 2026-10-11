/*
 * MediaStorageResultDTO：媒体模块的请求与传输数据文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 存储写入结果，只在存储实现与上传业务之间传递。
 *
 * storageKey 是相对 Key（形如 2026/10/<uuid>.png），禁止是服务器绝对路径。
 */
/**
 * 媒体存储结果传输对象，属于媒体模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaStorageResultDTO {

    /**
     * 存储提供方。
     */
    private String storageProvider;
    /**
     * 媒体在存储根目录内的相对键。
     */
    private String storageKey;
    /**
     * 文件大小，单位为字节。
     */
    private long sizeBytes;
}
