/*
 * MediaProperties：媒体模块的外部配置文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 承接配置文件和环境变量映射。字段初始值是默认值，实际部署可以通过配置绑定覆盖。
 * 主要协作类型：MediaType。
 * 阅读入口：maxBytesFor（按tes面向读取最大）。
 */

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
/**
 * 媒体配置属性，属于媒体模块。
 * 承接配置文件和环境变量映射。字段初始值是默认值，实际部署可以通过配置绑定覆盖。
 */
@Data
@ConfigurationProperties(prefix = "star-rain.media")
public class MediaProperties {

    // 本地存储根目录：与 frontend / backend 同级。数据库只保存相对 storageKey，不保存该绝对路径
    /**
     * 本地媒体存储根目录，实际使用时解析为绝对路径。
     */
    private String storageRoot = "../media";

    // 存储提供方标识，写入 sr_media_asset.storage_provider
    /**
     * 存储提供方。
     */
    private String storageProvider = "LOCAL";

    /**
     * 上传。
     */
    private Upload upload = new Upload();

    /**
     * 孤儿文件清理。
     */
    private OrphanCleanup orphanCleanup = new OrphanCleanup();

    /*
     * 各媒体大类的大小上限。
     * OTHER 取最严格的一档，避免出现“无人负责”的类型缺口。
     */
    /**
     * 按tes面向读取最大。
     * 关键处理步骤调用 <code>getUpload</code>、<code>limits.getMaxImageBytes</code>、<code>limits.getMaxDocumentBytes</code>、<code>limits.getMaxAudioBytes</code>。
     *
     * @param mediaType 媒体类型；类型为 <code>MediaType</code>
     * @return 类型为 <code>long</code> 的处理结果
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

    /**
     * 上传，属于媒体模块。
     * 承接配置文件和环境变量映射。字段初始值是默认值，实际部署可以通过配置绑定覆盖。
     */
    @Data
    public static class Upload {

        /*
         * 允许上传的扩展名白名单，默认覆盖图片、文档、音频、视频、压缩包五类。
         * 登记表里没有 svg，因此无论怎么配置都无法开放 SVG。
         */
        /**
         * allowedExtensions。
         */
        private List<String> allowedExtensions = new ArrayList<>(List.of(
                "png", "jpg", "jpeg", "webp", "gif", "bmp", "tiff", "tif", "avif",
                "pdf", "md", "markdown", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                "mp3", "m4a", "aac", "ogg", "oga", "wav", "flac",
                "mp4", "m4v", "mov", "webm", "mkv", "avi", "ogv",
                "zip", "rar", "7z", "tar", "gz", "tgz"));

        /**
         * 最大图片字节。
         */
        private long maxImageBytes = 20L * 1024 * 1024;

        /**
         * 最大文档字节。
         */
        private long maxDocumentBytes = 50L * 1024 * 1024;

        /**
         * 最大音频字节。
         */
        private long maxAudioBytes = 100L * 1024 * 1024;

        /**
         * 最大视频字节。
         */
        private long maxVideoBytes = 500L * 1024 * 1024;

        /**
         * 最大Archive字节。
         */
        private long maxArchiveBytes = 200L * 1024 * 1024;
    }

    /**
     * 孤儿文件清理，属于媒体模块。
     * 承接配置文件和环境变量映射。字段初始值是默认值，实际部署可以通过配置绑定覆盖。
     */
    @Data
    public static class OrphanCleanup {

        /**
         * 已启用。
         * 初始值为 <code>true</code>。
         */
        private boolean enabled = true;

        // 每天 03:30 执行
        /**
         * cron。
         */
        private String cron = "0 30 3 * * *";

        /*
         * 宽限期：只有写入时间早于该时长、且数据库无记录的孤儿文件才会被清理。
         * 这条规则防止误删“已经落盘、还没来得及写库”的正常上传文件。
         */
        /**
         * graceHours。
         * 初始值为 <code>24</code>。
         */
        private int graceHours = 24;
    }
}
