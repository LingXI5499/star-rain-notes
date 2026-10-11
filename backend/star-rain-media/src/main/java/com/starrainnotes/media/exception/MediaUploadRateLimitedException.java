/*
 * MediaUploadRateLimitedException：媒体模块的业务异常文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/**
 * 媒体上传RateLimited异常，属于媒体模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>MEDIA_UPLOAD_RATE_LIMITED</code>；HTTP 状态：<code>429</code>。
 * 公开提示：上传过于频繁，请稍后再试。
 */
public class MediaUploadRateLimitedException extends ApiException {

    /**
     * 构造MediaUploadRateLimitedException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public MediaUploadRateLimitedException() {
        super("MEDIA_UPLOAD_RATE_LIMITED", "上传过于频繁，请稍后再试", 429);
    }
}
