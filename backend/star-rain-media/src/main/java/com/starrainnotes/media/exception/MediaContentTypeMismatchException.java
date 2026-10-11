/*
 * MediaContentTypeMismatchException：媒体模块的业务异常文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 声明类型或真实内容与扩展名不符。
 *
 * 错误码 MEDIA_CONTENT_TYPE_MISMATCH，HTTP 415。
 */
/**
 * 媒体内容类型不匹配异常，属于媒体模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>MEDIA_CONTENT_TYPE_MISMATCH</code>；HTTP 状态：<code>415</code>。
 * 公开提示：声明的 Content-Type 或真实文件内容与扩展名不一致。
 */
public class MediaContentTypeMismatchException extends ApiException {

    /**
     * 构造MediaContentTypeMismatchException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public MediaContentTypeMismatchException() {
        super("MEDIA_CONTENT_TYPE_MISMATCH", "声明的 Content-Type 或真实文件内容与扩展名不一致", 415);
    }
}