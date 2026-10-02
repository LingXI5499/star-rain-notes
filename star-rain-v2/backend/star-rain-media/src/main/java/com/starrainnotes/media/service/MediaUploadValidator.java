package com.starrainnotes.media.service;

import com.starrainnotes.media.dto.UploadMetadata;
import org.springframework.web.multipart.MultipartFile;

/*
 * 上传元数据校验入口。
 *
 * 只校验“不读文件内容就能判断”的部分：非空、扩展名白名单、大小上限、声明 Content-Type。
 * 真实内容（magic bytes）与图片宽高由调用方在读完文件头/内容后，
 * 通过 FileSignatures 校验，从而避免大文件被整份读进内存。
 */
public interface MediaUploadValidator {

    // 校验失败时抛出对应的 MEDIA_* 业务错误
    UploadMetadata validate(MultipartFile file);
}
