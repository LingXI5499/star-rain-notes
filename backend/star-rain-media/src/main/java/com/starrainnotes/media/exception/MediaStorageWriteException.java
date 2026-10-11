/*
 * MediaStorageWriteException：媒体模块的业务异常文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 存储写入失败。
 *
 * 错误码 MEDIA_STORAGE_WRITE_FAILED，HTTP 500。
 */
/**
 * 媒体存储写入异常，属于媒体模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>MEDIA_STORAGE_WRITE_FAILED</code>；HTTP 状态：<code>500</code>。
 * 公开提示：媒体存储目录无法写入，请检查目录权限和磁盘空间。
 */
public class MediaStorageWriteException extends ApiException {

    /**
     * 构造MediaStorageWriteException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     *
     * @param cause 原始异常，用于保留失败原因；类型为 <code>Throwable</code>
     */
    public MediaStorageWriteException(Throwable cause) {
        super("MEDIA_STORAGE_WRITE_FAILED", "媒体存储目录无法写入，请检查目录权限和磁盘空间", 500, cause);
    }
}
