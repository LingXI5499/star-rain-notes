/*
 * MediaStorageDeleteException：媒体模块的业务异常文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 存储删除失败。
 *
 * 错误码 MEDIA_STORAGE_DELETE_FAILED，HTTP 500。
 */
/**
 * 媒体存储删除异常，属于媒体模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 */
public class MediaStorageDeleteException extends ApiException {

    /**
     * 构造MediaStorageDeleteException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>、<code>cause.getMessage</code>。
     *
     * @param cause 原始异常，用于保留失败原因；类型为 <code>Throwable</code>
     */
    public MediaStorageDeleteException(Throwable cause) {
        super("MEDIA_STORAGE_DELETE_FAILED", "媒体文件删除失败：" + cause.getMessage(), 500, cause);
    }
}