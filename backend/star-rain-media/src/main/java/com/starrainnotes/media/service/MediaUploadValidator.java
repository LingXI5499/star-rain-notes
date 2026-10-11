/*
 * MediaUploadValidator：媒体模块的业务契约文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：UploadMetadata。
 * 阅读入口：validate（校验媒体上传校验器）。
 */

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
/**
 * 媒体上传校验器，属于媒体模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface MediaUploadValidator {

    // 校验失败时抛出对应的 MEDIA_* 业务错误
    /**
     * 校验媒体上传校验器。
     *
     * @param file 待处理的上传文件；类型为 <code>MultipartFile</code>
     * @return 类型为 <code>UploadMetadata</code> 的处理结果
     */
    UploadMetadata validate(MultipartFile file);
}
