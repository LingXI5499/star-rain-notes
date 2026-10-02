package com.starrainnotes.media.dto;

import com.starrainnotes.media.enumeration.MediaType;
import com.starrainnotes.media.enumeration.MediaFileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 通过元数据校验的上传信息（不含文件内容）。
 *
 * 上传流程刻意把“元数据校验”和“内容校验”分开：
 * 元数据只用 MultipartFile 的文件名/声明类型/大小，代价极低；
 * 内容校验需要读取文件头，甚至整份内容（图片解析宽高）。
 *
 * 这样视频这类大文件就不必整份读进内存。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadMetadata {

    private String originalName;
    private String fileExtension;
    private MediaFileType fileType;
    private long sizeBytes;
    private long maxBytes;

    public MediaType mediaType() {
        return fileType.mediaType();
    }

    public String mimeType() {
        return fileType.canonicalMimeType();
    }

    public boolean requiresFullContent() {
        return fileType.requiresFullContent();
    }
}
