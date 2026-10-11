/*
 * MediaAccessDeniedException：媒体模块的业务异常文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 受保护媒体的读取授权失败。
 *
 * 错误码 MEDIA_ACCESS_DENIED，HTTP 403。
 */
/**
 * 媒体访问被拒绝异常，属于媒体模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 错误码：<code>MEDIA_ACCESS_DENIED</code>；HTTP 状态：<code>403</code>。
 * 公开提示：没有读取该媒体资源的权限。
 */
public class MediaAccessDeniedException extends ApiException {

    /**
     * 构造MediaAccessDeniedException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     */
    public MediaAccessDeniedException() {
        super("MEDIA_ACCESS_DENIED", "没有读取该媒体资源的权限", 403);
    }
}