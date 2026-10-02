package com.starrainnotes.media.utils;

import com.starrainnotes.media.utils.FileSignatures;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.starrainnotes.media.TestMediaFiles;
import com.starrainnotes.media.enumeration.MediaFileType;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/*
 * 真实文件内容校验测试：这是“不信任扩展名”的最终防线。
 * 每个媒体大类都要有用例，防止新增类型时只登记不校验。
 */
class FileSignaturesTest {

    @Test
    @DisplayName("图片：PNG / JPEG / GIF / BMP 读出宽高")
    void imageDimensions() throws IOException {
        assertThat(FileSignatures.verify(MediaFileType.PNG, TestMediaFiles.png(120, 80)))
                .containsExactly(120, 80);
        assertThat(FileSignatures.verify(MediaFileType.JPG, TestMediaFiles.jpeg(60, 40)))
                .containsExactly(60, 40);
        assertThat(FileSignatures.verify(MediaFileType.GIF, TestMediaFiles.gif(33, 22)))
                .containsExactly(33, 22);
        assertThat(FileSignatures.verify(MediaFileType.BMP, TestMediaFiles.bmp(16, 12)))
                .containsExactly(16, 12);
    }

    @Test
    @DisplayName("图片：WebP 解析 VP8X 容器头")
    void webpDimensions() throws IOException {
        assertThat(FileSignatures.verify(MediaFileType.WEBP, TestMediaFiles.webpVp8x(64, 32)))
                .containsExactly(64, 32);
    }

    @Test
    @DisplayName("图片：AVIF 校验 ISO BMFF 主品牌，不解析尺寸")
    void avifBrand() {
        assertThatCode(() -> FileSignatures.verify(MediaFileType.AVIF, TestMediaFiles.avif()))
                .doesNotThrowAnyException();
        // 品牌不匹配的 ftyp 容器不能当作 avif
        assertThatThrownBy(() -> FileSignatures.verify(MediaFileType.AVIF, TestMediaFiles.mp4()))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("文档：PDF / Markdown / OLE2 / OOXML 全部通过")
    void documents() throws IOException {
        assertThatCode(() -> FileSignatures.verify(MediaFileType.PDF, TestMediaFiles.pdf()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.MD, TestMediaFiles.markdown()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.DOC, TestMediaFiles.ole2Document()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.DOCX, TestMediaFiles.ooxmlDocument()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.XLSX, TestMediaFiles.ooxmlDocument()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("文档：文本类型拒绝二进制内容")
    void plainTextRejectsBinary() throws IOException {
        byte[] binary = TestMediaFiles.png(4, 4);
        assertThatThrownBy(() -> FileSignatures.verify(MediaFileType.TXT, binary))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("音频：MP3 / M4A / AAC / OGG / WAV / FLAC 全部通过")
    void audio() throws IOException {
        assertThatCode(() -> FileSignatures.verify(MediaFileType.MP3, TestMediaFiles.mp3()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.M4A, TestMediaFiles.m4a()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.AAC, TestMediaFiles.aac()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.OGG, TestMediaFiles.ogg()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.WAV, TestMediaFiles.wav()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.FLAC, TestMediaFiles.flac()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("视频：MP4 / MOV / WebM / AVI 全部通过")
    void video() throws IOException {
        assertThatCode(() -> FileSignatures.verify(MediaFileType.MP4, TestMediaFiles.mp4()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.MOV, TestMediaFiles.mov()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.WEBM, TestMediaFiles.webm()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.MKV, TestMediaFiles.webm()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.AVI, TestMediaFiles.avi()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("压缩包：ZIP / RAR / 7Z / TAR / GZ 全部通过")
    void archives() throws IOException {
        assertThatCode(() -> FileSignatures.verify(MediaFileType.ZIP, TestMediaFiles.zip()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.RAR, TestMediaFiles.rar()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.SEVEN_Z, TestMediaFiles.sevenZip()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.TAR, TestMediaFiles.tar()))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileSignatures.verify(MediaFileType.GZ, TestMediaFiles.gzip()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("换扩展名不改内容会被识别：PNG 内容改成 jpg 扩展名仍按 JPEG 校验失败")
    void rejectsContentThatDoesNotMatchExtension() throws IOException {
        byte[] pngBytes = TestMediaFiles.png(8, 8);
        assertThatThrownBy(() -> FileSignatures.verify(MediaFileType.JPG, pngBytes))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> FileSignatures.verify(MediaFileType.PDF, pngBytes))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> FileSignatures.verify(MediaFileType.ZIP, pngBytes))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("空内容不会通过任何类型的校验")
    void rejectsEmptyContent() {
        byte[] empty = new byte[0];
        assertThatThrownBy(() -> FileSignatures.verify(MediaFileType.PNG, empty))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> FileSignatures.verify(MediaFileType.MP4, empty))
                .isInstanceOf(IOException.class);
    }

    @ParameterizedTest
    @EnumSource(MediaFileType.class)
    @DisplayName("每个已登记的扩展名都有对应的类型与 MIME，不存在只登记不校验的条目")
    void everyRegisteredTypeHasMetadata(MediaFileType type) {
        assertThat(type.extension()).isNotBlank();
        assertThat(type.mediaType()).isNotNull();
        assertThat(type.canonicalMimeType()).contains("/");
        assertThat(MediaFileType.byExtension(type.extension())).contains(type);
    }
}
