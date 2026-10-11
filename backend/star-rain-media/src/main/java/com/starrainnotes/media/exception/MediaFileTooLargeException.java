/*
 * MediaFileTooLargeException：媒体模块的业务异常文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 主要协作类型：ApiException。
 * 阅读入口：describeLimit（describe上限处理入口）。
 */

package com.starrainnotes.media.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 文件超过该媒体大类的大小上限。
 *
 * 错误码 MEDIA_FILE_TOO_LARGE，HTTP 413。
 */
/**
 * 媒体文件TooLarge异常，属于媒体模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 */
public class MediaFileTooLargeException extends ApiException {

    /**
     * 构造MediaFileTooLargeException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>、<code>describeLimit</code>。
     *
     * @param maxBytes 最大字节；类型为 <code>long</code>
     */
    public MediaFileTooLargeException(long maxBytes) {
        super("MEDIA_FILE_TOO_LARGE", "文件超过允许大小 " + describeLimit(maxBytes), 413);
    }

    // 小于 1MB 时按 KB 展示，避免出现“0MB”这种无意义提示
    /**
     * describe上限处理入口。
     * 关键处理步骤调用 <code>Math.max</code>。
     *
     * @param bytes 字节；类型为 <code>long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private static String describeLimit(long bytes) {
        long mb = bytes / (1024 * 1024);
        return mb >= 1 ? mb + "MB" : Math.max(1, bytes / 1024) + "KB";
    }
}