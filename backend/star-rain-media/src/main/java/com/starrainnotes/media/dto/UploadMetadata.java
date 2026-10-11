/*
 * UploadMetadata：媒体模块的请求与传输数据文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 * 主要协作类型：MediaType、MediaFileType。
 * 阅读入口：mediaType（媒体类型处理入口）；mimeType（文件的 MIME 类型处理入口）；requiresFullContent（requires完整内容处理入口）。
 */

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
/**
 * 上传元数据，属于媒体模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadMetadata {

    /**
     * 原始名称。
     */
    private String originalName;
    /**
     * 文件扩展名。
     */
    private String fileExtension;
    /**
     * 文件类型。
     */
    private MediaFileType fileType;
    /**
     * 文件大小，单位为字节。
     */
    private long sizeBytes;
    /**
     * 最大字节。
     */
    private long maxBytes;

    /**
     * 媒体类型处理入口。
     * 主要协作调用：<code>fileType.mediaType</code>。
     * @return 类型为 <code>MediaType</code> 的处理结果
     */
    public MediaType mediaType() {
        return fileType.mediaType();
    }

    /**
     * 文件的 MIME 类型处理入口。
     * 主要协作调用：<code>fileType.canonicalMimeType</code>。
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String mimeType() {
        return fileType.canonicalMimeType();
    }

    /**
     * requires完整内容处理入口。
     * 主要协作调用：<code>fileType.requiresFullContent</code>。
     * @return 本方法条件是否成立的判断结果
     */
    public boolean requiresFullContent() {
        return fileType.requiresFullContent();
    }
}
