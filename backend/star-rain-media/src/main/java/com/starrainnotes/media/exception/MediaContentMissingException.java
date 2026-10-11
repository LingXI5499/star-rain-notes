/*
 * MediaContentMissingException：媒体模块的业务异常文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 数据库有记录但文件缺失，需运维介入。
 *
 * 错误码 MEDIA_CONTENT_MISSING，HTTP 500。
 */
/**
 * 媒体内容缺失异常，属于媒体模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>MEDIA_CONTENT_MISSING</code>；HTTP 状态：<code>500</code>。
 * 公开提示：媒体文件在存储中缺失。
 */
public class MediaContentMissingException extends ApiException {

    /**
     * 构造MediaContentMissingException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public MediaContentMissingException() {
        super("MEDIA_CONTENT_MISSING", "媒体文件在存储中缺失", 500);
    }
}