/*
 * MediaType：媒体模块的枚举文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 * 阅读入口：of（所属处理入口）。
 */

package com.starrainnotes.media.enumeration;

/*
 * 媒体大类。
 *
 * 用于后台筛选、上传大小上限与前端分类展示，不代表具体格式（格式看 fileExtension / mimeType）。
 * 前端按这个枚举分类渲染：图片走 <img>，音频走 <audio>，视频走 <video>，
 * 文档与压缩包走图标 + 下载入口。
 */
/**
 * 媒体类型，属于媒体模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum MediaType {
    /**
     * 图片。
     */
    IMAGE,
    /**
     * 文档。
     */
    DOCUMENT,
    /**
     * 音频。
     */
    AUDIO,
    /**
     * 视频。
     */
    VIDEO,
    /**
     * ARCHIVE。
     */
    ARCHIVE,
    /**
     * OTHER。
     */
    OTHER;

    // 宽松解析：空白或无法识别返回 null，让上层决定是拒绝还是忽略该筛选条件
    /**
     * 所属处理入口。
     * 关键处理步骤调用 <code>value.isBlank</code>、<code>MediaType.valueOf</code>、<code>value.trim</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>MediaType</code> 的处理结果
     */
    public static MediaType of(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MediaType.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
