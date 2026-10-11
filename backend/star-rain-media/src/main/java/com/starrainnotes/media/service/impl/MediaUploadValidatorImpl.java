/*
 * MediaUploadValidatorImpl：媒体模块的业务实现文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：MediaContentTypeMismatchException、MediaFileEmptyException、MediaFileTooLargeException、MediaTypeNotAllowedException、MediaProperties、MediaUploadValidator、MediaFileType。
 * 阅读入口：validate（校验媒体上传校验器实现）。
 */

package com.starrainnotes.media.service.impl;

import com.starrainnotes.media.exception.MediaContentTypeMismatchException;
import com.starrainnotes.media.exception.MediaFileEmptyException;
import com.starrainnotes.media.exception.MediaFileTooLargeException;
import com.starrainnotes.media.exception.MediaTypeNotAllowedException;
import com.starrainnotes.media.properties.MediaProperties;
import com.starrainnotes.media.service.MediaUploadValidator;
import com.starrainnotes.media.enumeration.MediaFileType;
import com.starrainnotes.media.dto.UploadMetadata;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/*
 * 上传元数据校验实现。
 *
 * 顺序有意为之：先做便宜且明确的拒绝（非空 → 白名单 → 大小 → 声明类型），
 * 最后才由调用方读文件内容做 magic bytes 校验。
 * 这里完全不读取文件内容，因此视频这类大文件不会因为校验而占内存。
 */
/**
 * 媒体上传校验器实现，属于媒体模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Component
public class MediaUploadValidatorImpl implements MediaUploadValidator {

    /**
     * LENGTH。
     * 初始值为 <code>255</code>。
     */
    private static final int MAX_NAME_LENGTH = 255;

    /**
     * 本模块的外部配置属性。
     * 协作类型：<code>MediaProperties</code>。
     */
    private final MediaProperties properties;

    /**
     * 构造MediaUploadValidatorImpl，接收本类使用的依赖或初始化参数。
     *
     * @param properties 本模块的外部配置属性；类型为 <code>MediaProperties</code>
     */
    public MediaUploadValidatorImpl(MediaProperties properties) {
        this.properties = properties;
    }

    /**
     * 校验媒体上传校验器实现。
     * 主要协作调用：<code>properties.getUpload</code>、<code>properties.maxBytesFor</code>。
     * 实现中的显式失败类型包括 <code>MediaFileEmptyException</code>、<code>MediaTypeNotAllowedException</code>、<code>MediaFileTooLargeException</code>、<code>MediaContentTypeMismatchException</code>；具体触发条件见对应分支。
     *
     * @param file 待处理的上传文件；类型为 <code>MultipartFile</code>
     * @return 类型为 <code>UploadMetadata</code> 的处理结果
     */
    @Override
    public UploadMetadata validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new MediaFileEmptyException();
        }

        String originalName = cleanName(file.getOriginalFilename());
        String extension = MediaFileType.extensionOf(originalName);
        List<String> allowed = properties.getUpload().getAllowedExtensions();
        if (extension.isEmpty() || allowed == null || !allowed.contains(extension)) {
            throw new MediaTypeNotAllowedException();
        }
        MediaFileType fileType = MediaFileType.byExtension(extension)
                .orElseThrow(MediaTypeNotAllowedException::new);

        long maxBytes = properties.maxBytesFor(fileType.mediaType());
        if (file.getSize() > maxBytes) {
            throw new MediaFileTooLargeException(maxBytes);
        }
        if (!fileType.acceptsDeclaredContentType(file.getContentType())) {
            throw new MediaContentTypeMismatchException();
        }

        return new UploadMetadata(originalName, extension, fileType, file.getSize(), maxBytes);
    }

    /*
     * 只保留最后一段文件名，去掉目录分隔符与控制字符。
     * 原始文件名允许重复；真实存储名由 Storage 用 UUID 生成，因此这里不参与路径构造。
     */
    /**
     * 清理名称处理入口。
     * 关键处理步骤调用 <code>raw.isBlank</code>、<code>name.replace</code>、<code>name.lastIndexOf</code>、<code>name.substring</code>。
     *
     * @param raw 原始；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    static String cleanName(String raw) {
        String name = (raw == null || raw.isBlank()) ? "file" : raw;
        name = name.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("\\p{Cntrl}", "").trim();
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
            name = "file";
        }
        // 超长时保留扩展名截断主干，避免撑爆 VARCHAR(255)
        if (name.length() > MAX_NAME_LENGTH) {
            String extension = MediaFileType.extensionOf(name);
            int keep = MAX_NAME_LENGTH - (extension.isEmpty() ? 0 : extension.length() + 1);
            name = name.substring(0, Math.max(1, keep)) + (extension.isEmpty() ? "" : "." + extension);
        }
        return name;
    }
}
