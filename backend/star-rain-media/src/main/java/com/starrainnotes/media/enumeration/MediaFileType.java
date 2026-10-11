/*
 * MediaFileType：媒体模块的枚举文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 * 阅读入口：extension（扩展名处理入口）；mediaType（媒体类型处理入口）；canonicalMimeType（规范MIME类型处理入口）；requiresFullContent（requires完整内容处理入口）。
 */

package com.starrainnotes.media.enumeration;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * 扩展名登记表：扩展名 -> 媒体大类 + 规范 MIME + 可接受的声明 Content-Type。
 *
 * 这是技术事实登记，不是业务策略：哪些扩展名真正开放由
 * star-rain.media.upload.allowed-extensions 配置决定。
 *
 * 不登记 svg：矢量格式可携带脚本，作为可内联展示的资源风险过高。
 *
 * 关于容器格式：doc/docx/xls/xlsx/ppt/pptx 与 zip 同属容器家族，
 * magic bytes 只能确认“容器合法”，无法区分同族内的具体子类型。
 * 因此这些类型的校验结论是“容器通过”，不是“子类型已证实”。
 */
/**
 * 媒体文件类型，属于媒体模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum MediaFileType {

    // ---------- 图片 ----------
    /**
     * PNG。
     */
    PNG("png", MediaType.IMAGE, "image/png", List.of("image/png")),
    /**
     * JPG。
     */
    JPG("jpg", MediaType.IMAGE, "image/jpeg", List.of("image/jpeg")),
    /**
     * JPEG。
     */
    JPEG("jpeg", MediaType.IMAGE, "image/jpeg", List.of("image/jpeg")),
    /**
     * WEBP。
     */
    WEBP("webp", MediaType.IMAGE, "image/webp", List.of("image/webp")),
    /**
     * GIF。
     */
    GIF("gif", MediaType.IMAGE, "image/gif", List.of("image/gif")),
    /**
     * BMP。
     */
    BMP("bmp", MediaType.IMAGE, "image/bmp", List.of("image/bmp", "image/x-ms-bmp")),
    /**
     * TIFF。
     */
    TIFF("tiff", MediaType.IMAGE, "image/tiff", List.of("image/tiff")),
    /**
     * TIF。
     */
    TIF("tif", MediaType.IMAGE, "image/tiff", List.of("image/tiff")),
    /**
     * AVIF。
     */
    AVIF("avif", MediaType.IMAGE, "image/avif", List.of("image/avif")),

    // ---------- 文档 ----------
    /**
     * PDF。
     */
    PDF("pdf", MediaType.DOCUMENT, "application/pdf", List.of("application/pdf")),
    /**
     * MD。
     */
    MD("md", MediaType.DOCUMENT, "text/markdown",
            List.of("text/markdown", "text/x-markdown", "text/plain", "application/octet-stream")),
    /**
     * Markdown。
     */
    MARKDOWN("markdown", MediaType.DOCUMENT, "text/markdown",
            List.of("text/markdown", "text/x-markdown", "text/plain", "application/octet-stream")),
    /**
     * TXT。
     */
    TXT("txt", MediaType.DOCUMENT, "text/plain", List.of("text/plain", "application/octet-stream")),
    /**
     * DOC。
     */
    DOC("doc", MediaType.DOCUMENT, "application/msword",
            List.of("application/msword", "application/octet-stream")),
    /**
     * DOCX。
     */
    DOCX("docx", MediaType.DOCUMENT,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            List.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/zip", "application/octet-stream")),
    /**
     * XLS。
     */
    XLS("xls", MediaType.DOCUMENT, "application/vnd.ms-excel",
            List.of("application/vnd.ms-excel", "application/octet-stream")),
    /**
     * XLSX。
     */
    XLSX("xlsx", MediaType.DOCUMENT,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            List.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/zip", "application/octet-stream")),
    /**
     * PPT。
     */
    PPT("ppt", MediaType.DOCUMENT, "application/vnd.ms-powerpoint",
            List.of("application/vnd.ms-powerpoint", "application/octet-stream")),
    /**
     * PPTX。
     */
    PPTX("pptx", MediaType.DOCUMENT,
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            List.of("application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/zip", "application/octet-stream")),

    // ---------- 音频 ----------
    /**
     * 3。
     */
    MP3("mp3", MediaType.AUDIO, "audio/mpeg", List.of("audio/mpeg", "audio/mp3")),
    /**
     * 4A。
     */
    M4A("m4a", MediaType.AUDIO, "audio/mp4", List.of("audio/mp4", "audio/x-m4a")),
    /**
     * AAC。
     */
    AAC("aac", MediaType.AUDIO, "audio/aac", List.of("audio/aac", "audio/x-aac")),
    /**
     * OGG。
     */
    OGG("ogg", MediaType.AUDIO, "audio/ogg", List.of("audio/ogg", "application/ogg")),
    /**
     * OGA。
     */
    OGA("oga", MediaType.AUDIO, "audio/ogg", List.of("audio/ogg", "application/ogg")),
    /**
     * WAV。
     */
    WAV("wav", MediaType.AUDIO, "audio/wav", List.of("audio/wav", "audio/x-wav", "audio/wave")),
    /**
     * FLAC。
     */
    FLAC("flac", MediaType.AUDIO, "audio/flac", List.of("audio/flac", "audio/x-flac")),

    // ---------- 视频 ----------
    /**
     * 4。
     */
    MP4("mp4", MediaType.VIDEO, "video/mp4", List.of("video/mp4")),
    /**
     * 4V。
     */
    M4V("m4v", MediaType.VIDEO, "video/x-m4v", List.of("video/x-m4v", "video/mp4")),
    /**
     * MOV。
     */
    MOV("mov", MediaType.VIDEO, "video/quicktime", List.of("video/quicktime")),
    /**
     * WEBM。
     */
    WEBM("webm", MediaType.VIDEO, "video/webm", List.of("video/webm")),
    /**
     * MKV。
     */
    MKV("mkv", MediaType.VIDEO, "video/x-matroska",
            List.of("video/x-matroska", "video/mkv", "application/octet-stream")),
    /**
     * AVI。
     */
    AVI("avi", MediaType.VIDEO, "video/x-msvideo",
            List.of("video/x-msvideo", "video/avi", "application/octet-stream")),
    /**
     * OGV。
     */
    OGV("ogv", MediaType.VIDEO, "video/ogg", List.of("video/ogg", "application/ogg")),

    // ---------- 压缩包 ----------
    /**
     * ZIP。
     */
    ZIP("zip", MediaType.ARCHIVE, "application/zip",
            List.of("application/zip", "application/x-zip-compressed", "application/octet-stream")),
    /**
     * RAR。
     */
    RAR("rar", MediaType.ARCHIVE, "application/vnd.rar",
            List.of("application/vnd.rar", "application/x-rar-compressed", "application/octet-stream")),
    /**
     * Z。
     */
    SEVEN_Z("7z", MediaType.ARCHIVE, "application/x-7z-compressed",
            List.of("application/x-7z-compressed", "application/octet-stream")),
    /**
     * TAR。
     */
    TAR("tar", MediaType.ARCHIVE, "application/x-tar",
            List.of("application/x-tar", "application/octet-stream")),
    /**
     * GZ。
     */
    GZ("gz", MediaType.ARCHIVE, "application/gzip",
            List.of("application/gzip", "application/x-gzip", "application/octet-stream")),
    /**
     * TGZ。
     */
    TGZ("tgz", MediaType.ARCHIVE, "application/gzip",
            List.of("application/gzip", "application/x-gzip", "application/octet-stream"));

    /**
     * 扩展名。
     */
    private static final Map<String, MediaFileType> BY_EXTENSION = Stream.of(values())
            .collect(Collectors.toMap(MediaFileType::extension, type -> type));

    /**
     * 扩展名。
     */
    private final String extension;
    /**
     * 媒体类型。
     */
    private final MediaType mediaType;
    /**
     * 规范MIME类型。
     */
    private final String canonicalMimeType;
    /**
     * 已接受内容Types。
     */
    private final List<String> acceptedContentTypes;

    /**
     * 构造MediaFileType，接收本类使用的依赖或初始化参数。
     *
     * @param extension 扩展名；类型为 <code>String</code>
     * @param mediaType 媒体类型；类型为 <code>MediaType</code>
     * @param canonicalMimeType 规范MIME类型；类型为 <code>String</code>
     * @param acceptedContentTypes 已接受内容Types；类型为 <code>List&lt;String&gt;</code>
     */
    MediaFileType(String extension, MediaType mediaType, String canonicalMimeType,
                  List<String> acceptedContentTypes) {
        this.extension = extension;
        this.mediaType = mediaType;
        this.canonicalMimeType = canonicalMimeType;
        this.acceptedContentTypes = acceptedContentTypes;
    }

    /**
     * 扩展名处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String extension() {
        return extension;
    }

    /**
     * 媒体类型处理入口。
     * @return 类型为 <code>MediaType</code> 的处理结果
     */
    public MediaType mediaType() {
        return mediaType;
    }

    // 入库时使用的规范 MIME，来自登记表而不是浏览器声明
    /**
     * 规范MIME类型处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String canonicalMimeType() {
        return canonicalMimeType;
    }

    /*
     * 图片要解析宽高，所以整份读入内存（图片本身有 10MB 上限）；
     * 视频/音频/压缩包可能很大，只读文件头做签名校验，其余流式落盘。
     */
    /**
     * requires完整内容处理入口。
     * @return 本方法条件是否成立的判断结果
     */
    public boolean requiresFullContent() {
        return mediaType == MediaType.IMAGE;
    }

    /**
     * 依据扩展名处理入口。
     * 主要协作调用：<code>extension.isBlank</code>、<code>BY_EXTENSION.get</code>、<code>extension.trim</code>。
     *
     * @param extension 扩展名；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    public static Optional<MediaFileType> byExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_EXTENSION.get(extension.trim().toLowerCase(Locale.ROOT)));
    }

    // 规范化声明 Content-Type：去掉 charset 等参数并转小写
    /**
     * 规范化内容类型。
     * 关键处理步骤调用 <code>contentType.indexOf</code>、<code>contentType.substring</code>、<code>head.trim</code>。
     *
     * @param contentType 内容类型或请求声明的 Content-Type；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        String head = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
        return head.trim().toLowerCase(Locale.ROOT);
    }

    /*
     * 声明 Content-Type 是否可接受。
     *
     * 空值与 application/octet-stream 视为「客户端未声明具体类型」：
     * 命令行工具、脚本、部分客户端上传时会这样发送，而浏览器会带具体类型。
     * 这类情况放行，交给 magic bytes 判定真实内容——
     * 真实内容校验才是权威，声明类型只是提前发现明显不一致的辅助检查。
     *
     * 注意：声明了具体类型就必须与扩展名一致，所以把 jpg 改名成 png
     * 依然会在后续的 magic bytes 校验里被拒绝。
     */
    /**
     * acceptsDeclared内容类型处理入口。
     * 主要协作调用：<code>acceptedContentTypes.contains</code>。
     *
     * @param declaredContentType declared内容类型；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    public boolean acceptsDeclaredContentType(String declaredContentType) {
        String normalized = normalizeContentType(declaredContentType);
        if (normalized.isEmpty() || "application/octet-stream".equals(normalized)) {
            return true;
        }
        return acceptedContentTypes.contains(normalized);
    }

    // 取扩展名（不含点，小写）。没有扩展名返回空串，由调用方决定是否拒绝
    /**
     * 扩展名所属处理入口。
     * 关键处理步骤调用 <code>filename.lastIndexOf</code>、<code>filename.length</code>、<code>filename.substring</code>。
     *
     * @param filename 文件名；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
