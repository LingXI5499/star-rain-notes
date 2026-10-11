/*
 * MediaContentVO：媒体模块的响应数据文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 * 主要协作类型：HttpByteRange、MediaType。
 * 阅读入口：partial（partial处理入口）；contentLength（内容Length处理入口）；inlineDisplay（inline显示处理入口）。
 */

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
/**
 * 媒体内容响应对象，属于媒体模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaContentVO {

    /**
     * 关联的媒体资产标识。
     */
    private Long mediaAssetId;
    /**
     * 媒体类型。
     */
    private MediaType mediaType;
    /**
     * 文件的 MIME 类型。
     */
    private String mimeType;
    /**
     * 原始名称。
     */
    private String originalName;
    /**
     * 总数大小。
     */
    private long totalSize;
    /**
     * 文件内容的 SHA-256 摘要。
     */
    private String sha256;
    /**
     * 媒体访问级别。
     */
    private String accessLevel;
    /**
     * 范围。
     */
    private HttpByteRange range;
    /**
     * 数据流。
     */
    private InputStream stream;

    /**
     * partial处理入口。
     * @return 本方法条件是否成立的判断结果
     */
    public boolean partial() {
        return range != null;
    }

    /**
     * 内容Length处理入口。
     * 主要协作调用：<code>range.length</code>。
     * @return 类型为 <code>long</code> 的处理结果
     */
    public long contentLength() {
        return range == null ? totalSize : range.length();
    }

    /**
     * inline显示处理入口。
     * @return 本方法条件是否成立的判断结果
     */
    public boolean inlineDisplay() {
        return mediaType == MediaType.IMAGE
                || mediaType == MediaType.AUDIO
                || mediaType == MediaType.VIDEO;
    }
}
