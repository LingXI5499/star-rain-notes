package com.starrainnotes.media.properties;

import com.starrainnotes.media.enumeration.MediaType;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/*
 * Media 模块配置。
 *
 * 白名单与各类型大小上限都由配置决定，不硬编码进 Use Case；
 * 扩展名到 MIME 与 magic bytes 的对应关系属于技术事实，保留在代码里。
 */
@Data
@ConfigurationProperties(prefix = "star-rain.media")
public class MediaProperties {

    // 本地存储根目录：与 frontend / backend 同级。数据库只保存相对 storageKey，不保存该绝对路径
    private String storageRoot = "../media";

    // 存储提供方标识，写入 sr_media_asset.storage_provider
    private String storageProvider = "LOCAL";

    private Upload upload = new Upload();

    private OrphanCleanup orphanCleanup = new OrphanCleanup();

    /*
     * 各媒体大类的大小上限。
     * OTHER 取最严格的一档，避免出现“无人负责”的类型缺口。
     */
    public long maxBytesFor(MediaType mediaType) {
        Upload limits = getUpload();
        return switch (mediaType) {
            case IMAGE -> limits.getMaxImageBytes();
            case DOCUMENT -> limits.getMaxDocumentBytes();
            case AUDIO -> limits.getMaxAudioBytes();
            case VIDEO -> limits.getMaxVideoBytes();
            case ARCHIVE -> limits.getMaxArchiveBytes();
            case OTHER -> Math.min(limits.getMaxDocumentBytes(), limits.getMaxImageBytes());
        };
    }

    @Data
    public static class Upload {

        /*
         * 允许上传的扩展名白名单，默认覆盖图片、文档、音频、视频、压缩包五类。
         * 登记表里没有 svg，因此无论怎么配置都无法开放 SVG。
         */
        private List<String> allowedExtensions = new ArrayList<>(List.of(
                "png", "jpg", "jpeg", "webp", "gif", "bmp", "tiff", "tif", "avif",
                "pdf", "md", "markdown", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                "mp3", "m4a", "aac", "ogg", "oga", "wav", "flac",
                "mp4", "m4v", "mov", "webm", "mkv", "avi", "ogv",
                "zip", "rar", "7z", "tar", "gz", "tgz"));

        private long maxImageBytes = 20L * 1024 * 1024;

        private long maxDocumentBytes = 50L * 1024 * 1024;

        private long maxAudioBytes = 100L * 1024 * 1024;

        private long maxVideoBytes = 500L * 1024 * 1024;

        private long maxArchiveBytes = 200L * 1024 * 1024;
    }

    @Data
    public static class OrphanCleanup {

        private boolean enabled = true;

        // 每天 03:30 执行
        private String cron = "0 30 3 * * *";

        /*
         * 宽限期：只有写入时间早于该时长、且数据库无记录的孤儿文件才会被清理。
         * 这条规则防止误删“已经落盘、还没来得及写库”的正常上传文件。
         */
        private int graceHours = 24;
    }
}
