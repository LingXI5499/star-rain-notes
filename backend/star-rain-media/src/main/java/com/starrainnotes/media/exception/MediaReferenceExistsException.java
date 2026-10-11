/*
 * MediaReferenceExistsException：媒体模块的业务异常文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 同一来源与用途的引用重复登记。
 *
 * 错误码 MEDIA_REFERENCE_EXISTS，HTTP 409。
 */
/**
 * 媒体引用存在性异常，属于媒体模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>MEDIA_REFERENCE_EXISTS</code>；HTTP 状态：<code>409</code>。
 * 公开提示：该媒体引用已经存在。
 */
public class MediaReferenceExistsException extends ApiException {

    /**
     * 构造MediaReferenceExistsException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public MediaReferenceExistsException() {
        super("MEDIA_REFERENCE_EXISTS", "该媒体引用已经存在", 409);
    }
}