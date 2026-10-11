/*
 * MediaAssetNotFoundException：媒体模块的业务异常文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 媒体资源不存在。
 *
 * 错误码 MEDIA_NOT_FOUND，HTTP 404。
 */
/**
 * 媒体资产不存在异常，属于媒体模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>MEDIA_NOT_FOUND</code>；HTTP 状态：<code>404</code>。
 * 公开提示：媒体资源不存在。
 */
public class MediaAssetNotFoundException extends ApiException {

    /**
     * 构造MediaAssetNotFoundException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public MediaAssetNotFoundException() {
        super("MEDIA_NOT_FOUND", "媒体资源不存在", 404);
    }
}