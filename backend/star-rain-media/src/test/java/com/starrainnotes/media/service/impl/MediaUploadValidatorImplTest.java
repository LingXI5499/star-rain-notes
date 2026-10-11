/*
 * MediaUploadValidatorImplTest：媒体模块的测试文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：ApiException、TestMediaFiles、MediaProperties、UploadMetadata、MediaType。
 * 阅读入口：setUp（初始化当前测试场景的被测对象与模拟依赖）；acceptsImage（\u56fe\u7247\uff1a\u901a\u8fc7\u6821\u9a8c\u5e76\u8bc6\u522b\u4e3a IMAGE）；acceptsDocuments（\u6587\u6863\uff1apdf / md / docx \u90fd\u8bc6\u522b\u4e3a DOCUMENT）；acceptsAudioAndVideo（\u97f3\u9891\u4e0e\u89c6\u9891\uff1a\u8bc6\u522b\u4e3a AUDIO / VIDEO\uff0c\u4e14\u4e0d\u9700\u8981\u6574\u4efd\u8bfb\u5165）。
 */

package com.starrainnotes.media.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.TestMediaFiles;
import com.starrainnotes.media.properties.MediaProperties;
import com.starrainnotes.media.dto.UploadMetadata;
import com.starrainnotes.media.enumeration.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/*
 * 上传元数据校验测试：MED-001 的第一道边界。
 * 覆盖空文件、扩展名白名单、各类型大小上限、声明 Content-Type 与文件名清洗。
 */
/**
 * 媒体上传校验器实现测试，属于媒体模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class MediaUploadValidatorImplTest {

    /**
     * 本模块的外部配置属性。
     * 协作类型：<code>MediaProperties</code>。
     */
    private MediaProperties properties;
    /**
     * 校验器。
     */
    private MediaUploadValidatorImpl validator;

    /**
     * 初始化当前测试场景的被测对象与模拟依赖。
     */
    @BeforeEach
    void setUp() {
        properties = new MediaProperties();
        validator = new MediaUploadValidatorImpl(properties);
    }

    /**
     * \u56fe\u7247\uff1a\u901a\u8fc7\u6821\u9a8c\u5e76\u8bc6\u522b\u4e3a IMAGE。
     * 主要协作调用：<code>validator.validate</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @DisplayName("图片：通过校验并识别为 IMAGE")
    void acceptsImage() throws IOException {
        UploadMetadata metadata = validator.validate(
                file("cover.png", "image/png", TestMediaFiles.png(120, 80)));

        assertThat(metadata.mediaType()).isEqualTo(MediaType.IMAGE);
        assertThat(metadata.mimeType()).isEqualTo("image/png");
        assertThat(metadata.getFileExtension()).isEqualTo("png");
        assertThat(metadata.requiresFullContent()).isTrue();
    }

    /**
     * \u6587\u6863\uff1apdf / md / docx \u90fd\u8bc6\u522b\u4e3a DOCUMENT。
     * 主要协作调用：<code>validator.validate</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @DisplayName("文档：pdf / md / docx 都识别为 DOCUMENT")
    void acceptsDocuments() throws IOException {
        assertThat(validator.validate(file("a.pdf", "application/pdf", TestMediaFiles.pdf())).mediaType())
                .isEqualTo(MediaType.DOCUMENT);
        assertThat(validator.validate(file("a.md", "text/markdown", TestMediaFiles.markdown())).mediaType())
                .isEqualTo(MediaType.DOCUMENT);
        assertThat(validator.validate(file("a.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                TestMediaFiles.ooxmlDocument())).mediaType()).isEqualTo(MediaType.DOCUMENT);
        assertThat(validator.validate(file("a.doc", "application/msword",
                TestMediaFiles.ole2Document())).mediaType()).isEqualTo(MediaType.DOCUMENT);
    }

    /**
     * \u97f3\u9891\u4e0e\u89c6\u9891\uff1a\u8bc6\u522b\u4e3a AUDIO / VIDEO\uff0c\u4e14\u4e0d\u9700\u8981\u6574\u4efd\u8bfb\u5165。
     * 主要协作调用：<code>validator.validate</code>。
     */
    @Test
    @DisplayName("音频与视频：识别为 AUDIO / VIDEO，且不需要整份读入")
    void acceptsAudioAndVideo() {
        UploadMetadata audio = validator.validate(file("a.mp3", "audio/mpeg", TestMediaFiles.mp3()));
        UploadMetadata video = validator.validate(file("a.mp4", "video/mp4", TestMediaFiles.mp4()));

        assertThat(audio.mediaType()).isEqualTo(MediaType.AUDIO);
        assertThat(video.mediaType()).isEqualTo(MediaType.VIDEO);
        // 视频可达 200MB，绝不能整份读进内存
        assertThat(video.requiresFullContent()).isFalse();
        assertThat(audio.requiresFullContent()).isFalse();
    }

    /**
     * \u538b\u7f29\u5305\uff1a\u8bc6\u522b\u4e3a ARCHIVE。
     * 主要协作调用：<code>validator.validate</code>。
     */
    @Test
    @DisplayName("压缩包：识别为 ARCHIVE")
    void acceptsArchive() {
        UploadMetadata metadata = validator.validate(
                file("a.zip", "application/zip", TestMediaFiles.zip()));

        assertThat(metadata.mediaType()).isEqualTo(MediaType.ARCHIVE);
    }

    /**
     * \u7a7a\u6587\u4ef6\u88ab\u62d2\u7edd。
     * 主要协作调用：<code>validator.validate</code>。
     */
    @Test
    @DisplayName("空文件被拒绝")
    void rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> validator.validate(empty))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_FILE_EMPTY");
    }

    /**
     * SVG \u4e0e\u672a\u767b\u8bb0\u6269\u5c55\u540d\u88ab\u62d2\u7edd\uff1a\u767b\u8bb0\u8868\u91cc\u6ca1\u6709 svg\uff0c\u914d\u7f6e\u4e5f\u65e0\u6cd5\u5f00\u653e。
     * 主要协作调用：<code>validator.validate</code>。
     */
    @Test
    @DisplayName("SVG 与未登记扩展名被拒绝：登记表里没有 svg，配置也无法开放")
    void rejectsUnregisteredExtension() {
        for (String name : new String[]{"icon.svg", "script.sh", "README"}) {
            MockMultipartFile file = new MockMultipartFile("file", name,
                    "application/octet-stream", "<x/>".getBytes(StandardCharsets.UTF_8));
            assertThatThrownBy(() -> validator.validate(file))
                    .as("name=%s", name)
                    .extracting(ex -> ((ApiException) ex).getCode())
                    .isEqualTo("MEDIA_TYPE_NOT_ALLOWED");
        }
    }

    /**
     * \u5927\u5c0f\u4e0a\u9650\u6309\u5a92\u4f53\u5927\u7c7b\u5206\u522b\u751f\u6548\uff1a\u89c6\u9891\u4e0a\u9650\u8fdc\u5927\u4e8e\u56fe\u7247。
     * 主要协作调用：<code>properties.getUpload</code>、<code>validator.validate</code>。
     */
    @Test
    @DisplayName("大小上限按媒体大类分别生效：视频上限远大于图片")
    void sizeLimitsArePerMediaType() {
        properties.getUpload().setMaxImageBytes(16);
        properties.getUpload().setMaxVideoBytes(1024);

        assertThatThrownBy(() -> validator.validate(
                file("big.png", "image/png", new byte[17])))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_FILE_TOO_LARGE");

        // 同样 17 字节对视频远未超限，说明上限是按类型判定的
        assertThat(validator.validate(file("clip.mp4", "video/mp4", TestMediaFiles.mp4())).mediaType())
                .isEqualTo(MediaType.VIDEO);
    }

    /**
     * \u58f0\u660e Content-Type \u4e0e\u6269\u5c55\u540d\u4e0d\u7b26\u88ab\u62d2\u7edd。
     * 主要协作调用：<code>validator.validate</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @DisplayName("声明 Content-Type 与扩展名不符被拒绝")
    void rejectsDeclaredContentTypeMismatch() throws IOException {
        assertThatThrownBy(() -> validator.validate(
                file("cover.png", "application/pdf", TestMediaFiles.png(8, 8))))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_CONTENT_TYPE_MISMATCH");
    }

    /**
     * \u539f\u59cb\u6587\u4ef6\u540d\u53bb\u6389\u8def\u5f84\u5206\u9694\u7b26\uff0c\u4e0d\u5141\u8bb8\u53c2\u4e0e\u8def\u5f84\u6784\u9020。
     * 主要协作调用：<code>validator.validate</code>。
     */
    @Test
    @DisplayName("原始文件名去掉路径分隔符，不允许参与路径构造")
    void sanitizesOriginalName() {
        MockMultipartFile traversal = new MockMultipartFile("file",
                "..\\..\\windows\\system32\\evil.png", "image/png", new byte[]{1});

        assertThat(validator.validate(traversal).getOriginalName()).isEqualTo("evil.png");
    }

    /**
     * \u8d85\u957f\u6587\u4ef6\u540d\u88ab\u622a\u65ad\u4f46\u4fdd\u7559\u6269\u5c55\u540d。
     * 主要协作调用：<code>validator.validate</code>。
     */
    @Test
    @DisplayName("超长文件名被截断但保留扩展名")
    void truncatesLongName() {
        String longName = "a".repeat(300) + ".png";
        MockMultipartFile file = new MockMultipartFile("file", longName, "image/png", new byte[]{1});

        UploadMetadata metadata = validator.validate(file);

        assertThat(metadata.getOriginalName()).hasSize(255).endsWith(".png");
        assertThat(metadata.getFileExtension()).isEqualTo("png");
    }

    /**
     * 待处理的上传文件处理入口。
     *
     * @param name 名称；类型为 <code>String</code>
     * @param contentType 内容类型或请求声明的 Content-Type；类型为 <code>String</code>
     * @param content 内容；类型为 <code>byte[]</code>
     * @return 类型为 <code>MockMultipartFile</code> 的处理结果
     */
    private static MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }
}
