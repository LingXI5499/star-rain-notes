package com.starrainnotes.media.enumeration;

/*
 * 媒体大类。
 *
 * 用于后台筛选、上传大小上限与前端分类展示，不代表具体格式（格式看 fileExtension / mimeType）。
 * 前端按这个枚举分类渲染：图片走 <img>，音频走 <audio>，视频走 <video>，
 * 文档与压缩包走图标 + 下载入口。
 */
public enum MediaType {
    IMAGE,
    DOCUMENT,
    AUDIO,
    VIDEO,
    ARCHIVE,
    OTHER;

    // 宽松解析：空白或无法识别返回 null，让上层决定是拒绝还是忽略该筛选条件
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
