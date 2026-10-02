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
class MediaUploadValidatorImplTest {

    private MediaProperties properties;
    private MediaUploadValidatorImpl validator;

    @BeforeEach
    void setUp() {
        properties = new MediaProperties();
        validator = new MediaUploadValidatorImpl(properties);
    }

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

    @Test
    @DisplayName("压缩包：识别为 ARCHIVE")
    void acceptsArchive() {
        UploadMetadata metadata = validator.validate(
                file("a.zip", "application/zip", TestMediaFiles.zip()));

        assertThat(metadata.mediaType()).isEqualTo(MediaType.ARCHIVE);
    }

    @Test
    @DisplayName("空文件被拒绝")
    void rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> validator.validate(empty))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_FILE_EMPTY");
    }

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

    @Test
    @DisplayName("声明 Content-Type 与扩展名不符被拒绝")
    void rejectsDeclaredContentTypeMismatch() throws IOException {
        assertThatThrownBy(() -> validator.validate(
                file("cover.png", "application/pdf", TestMediaFiles.png(8, 8))))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_CONTENT_TYPE_MISMATCH");
    }

    @Test
    @DisplayName("原始文件名去掉路径分隔符，不允许参与路径构造")
    void sanitizesOriginalName() {
        MockMultipartFile traversal = new MockMultipartFile("file",
                "..\\..\\windows\\system32\\evil.png", "image/png", new byte[]{1});

        assertThat(validator.validate(traversal).getOriginalName()).isEqualTo("evil.png");
    }

    @Test
    @DisplayName("超长文件名被截断但保留扩展名")
    void truncatesLongName() {
        String longName = "a".repeat(300) + ".png";
        MockMultipartFile file = new MockMultipartFile("file", longName, "image/png", new byte[]{1});

        UploadMetadata metadata = validator.validate(file);

        assertThat(metadata.getOriginalName()).hasSize(255).endsWith(".png");
        assertThat(metadata.getFileExtension()).isEqualTo("png");
    }

    private static MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }
}
