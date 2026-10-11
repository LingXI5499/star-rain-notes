/*
 * MediaFileEmptyException：媒体模块的业务异常文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 上传内容为空。
 *
 * 错误码 MEDIA_FILE_EMPTY，HTTP 400。
 */
/**
 * 媒体文件空白异常，属于媒体模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>MEDIA_FILE_EMPTY</code>；HTTP 状态：<code>400</code>。
 * 公开提示：上传文件为空。
 */
public class MediaFileEmptyException extends ApiException {

    /**
     * 构造MediaFileEmptyException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public MediaFileEmptyException() {
        super("MEDIA_FILE_EMPTY", "上传文件为空", 400);
    }
}