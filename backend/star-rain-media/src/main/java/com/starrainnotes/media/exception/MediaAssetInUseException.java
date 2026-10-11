/*
 * MediaAssetInUseException：媒体模块的业务异常文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 */

package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 存在业务引用时禁止归档。
 *
 * 错误码 MEDIA_ASSET_IN_USE，HTTP 409。
 */
/**
 * 媒体资产内部Use异常，属于媒体模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 */
public class MediaAssetInUseException extends ApiException {

    /**
     * 构造MediaAssetInUseException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     *
     * @param referenceCount 引用数量；类型为 <code>long</code>
     */
    public MediaAssetInUseException(long referenceCount) {
        super("MEDIA_ASSET_IN_USE", "媒体仍被 " + referenceCount + " 处业务引用，不能归档", 409);
    }
}