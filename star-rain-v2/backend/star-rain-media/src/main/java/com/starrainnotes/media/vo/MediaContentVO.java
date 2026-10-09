package com.starrainnotes.media.vo;

import com.starrainnotes.media.utils.HttpByteRange;
import com.starrainnotes.media.enumeration.MediaType;
import java.io.InputStream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 一次媒体读取的结果。
 *
 * stream 由调用方（Controller 的响应转换器）负责关闭；这里不缓存字节内容，
 * 因此 200MB 的视频也不会占用等量堆内存。
 *
 * range 为 null 表示完整内容（HTTP 200）；非 null 表示单区间响应（HTTP 206）。
 *
 * 展示策略：图片/音频/视频 inline，浏览器可直接播放；文档与压缩包走 attachment 下载，
 * 避免浏览器去渲染未知格式。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaContentVO {

    private Long mediaAssetId;
    private MediaType mediaType;
    private String mimeType;
    private String originalName;
    private long totalSize;
    private String sha256;
    private String accessLevel;
    private HttpByteRange range;
    private InputStream stream;

    public boolean partial() {
        return range != null;
    }

    public long contentLength() {
        return range == null ? totalSize : range.length();
    }

    public boolean inlineDisplay() {
        return mediaType == MediaType.IMAGE
                || mediaType == MediaType.AUDIO
                || mediaType == MediaType.VIDEO;
    }
}
