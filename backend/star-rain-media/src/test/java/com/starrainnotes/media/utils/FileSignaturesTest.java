/*
 * FileSignaturesTest：媒体模块的测试文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：FileSignatures、TestMediaFiles、MediaFileType。
 * 阅读入口：imageDimensions（\u56fe\u7247\uff1aPNG / JPEG / GIF / BMP \u8bfb\u51fa\u5bbd\u9ad8）；webpDimensions（\u56fe\u7247\uff1aWebP \u89e3\u6790 VP8X \u5bb9\u5668\u5934）；avifBrand（\u56fe\u7247\uff1aAVIF \u6821\u9a8c ISO BMFF \u4e3b\u54c1\u724c\uff0c\u4e0d\u89e3\u6790\u5c3a\u5bf8）；documents（\u6587\u6863\uff1aPDF / Markdown / OLE2 / OOXML \u5168\u90e8\u901a\u8fc7）。
 */

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
/**
 * 文件Signatures测试，属于媒体模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class FileSignaturesTest {

    /**
     * \u56fe\u7247\uff1aPNG / JPEG / GIF / BMP \u8bfb\u51fa\u5bbd\u9ad8。
     * 关键处理步骤调用 <code>FileSignatures.verify</code>、<code>TestMediaFiles.png</code>、<code>TestMediaFiles.jpeg</code>、<code>TestMediaFiles.gif</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
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

    /**
     * \u56fe\u7247\uff1aWebP \u89e3\u6790 VP8X \u5bb9\u5668\u5934。
     * 关键处理步骤调用 <code>FileSignatures.verify</code>、<code>TestMediaFiles.webpVp8x</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @DisplayName("图片：WebP 解析 VP8X 容器头")
    void webpDimensions() throws IOException {
        assertThat(FileSignatures.verify(MediaFileType.WEBP, TestMediaFiles.webpVp8x(64, 32)))
                .containsExactly(64, 32);
    }

    /**
     * \u56fe\u7247\uff1aAVIF \u6821\u9a8c ISO BMFF \u4e3b\u54c1\u724c\uff0c\u4e0d\u89e3\u6790\u5c3a\u5bf8。
     * 关键处理步骤调用 <code>FileSignatures.verify</code>、<code>TestMediaFiles.avif</code>、<code>TestMediaFiles.mp4</code>。
     */
    @Test
    @DisplayName("图片：AVIF 校验 ISO BMFF 主品牌，不解析尺寸")
    void avifBrand() {
        assertThatCode(() -> FileSignatures.verify(MediaFileType.AVIF, TestMediaFiles.avif()))
                .doesNotThrowAnyException();
        // 品牌不匹配的 ftyp 容器不能当作 avif
        assertThatThrownBy(() -> FileSignatures.verify(MediaFileType.AVIF, TestMediaFiles.mp4()))
                .isInstanceOf(IOException.class);
    }

    /**
     * \u6587\u6863\uff1aPDF / Markdown / OLE2 / OOXML \u5168\u90e8\u901a\u8fc7。
     * 关键处理步骤调用 <code>FileSignatures.verify</code>、<code>TestMediaFiles.pdf</code>、<code>TestMediaFiles.markdown</code>、<code>TestMediaFiles.ole2Document</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
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

    /**
     * \u6587\u6863\uff1a\u6587\u672c\u7c7b\u578b\u62d2\u7edd\u4e8c\u8fdb\u5236\u5185\u5bb9。
     * 关键处理步骤调用 <code>TestMediaFiles.png</code>、<code>FileSignatures.verify</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @DisplayName("文档：文本类型拒绝二进制内容")
    void plainTextRejectsBinary() throws IOException {
        byte[] binary = TestMediaFiles.png(4, 4);
        assertThatThrownBy(() -> FileSignatures.verify(MediaFileType.TXT, binary))
                .isInstanceOf(IOException.class);
    }

    /**
     * \u97f3\u9891\uff1aMP3 / M4A / AAC / OGG / WAV / FLAC \u5168\u90e8\u901a\u8fc7。
     * 关键处理步骤调用 <code>FileSignatures.verify</code>、<code>TestMediaFiles.mp3</code>、<code>TestMediaFiles.m4a</code>、<code>TestMediaFiles.aac</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
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

    /**
     * \u89c6\u9891\uff1aMP4 / MOV / WebM / AVI \u5168\u90e8\u901a\u8fc7。
     * 关键处理步骤调用 <code>FileSignatures.verify</code>、<code>TestMediaFiles.mp4</code>、<code>TestMediaFiles.mov</code>、<code>TestMediaFiles.webm</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
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

    /**
     * \u538b\u7f29\u5305\uff1aZIP / RAR / 7Z / TAR / GZ \u5168\u90e8\u901a\u8fc7。
     * 关键处理步骤调用 <code>FileSignatures.verify</code>、<code>TestMediaFiles.zip</code>、<code>TestMediaFiles.rar</code>、<code>TestMediaFiles.sevenZip</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
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

    /**
     * \u6362\u6269\u5c55\u540d\u4e0d\u6539\u5185\u5bb9\u4f1a\u88ab\u8bc6\u522b\uff1aPNG \u5185\u5bb9\u6539\u6210 jpg \u6269\u5c55\u540d\u4ecd\u6309 JPEG \u6821\u9a8c\u5931\u8d25。
     * 关键处理步骤调用 <code>TestMediaFiles.png</code>、<code>FileSignatures.verify</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
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

    /**
     * \u7a7a\u5185\u5bb9\u4e0d\u4f1a\u901a\u8fc7\u4efb\u4f55\u7c7b\u578b\u7684\u6821\u9a8c。
     * 关键处理步骤调用 <code>FileSignatures.verify</code>。
     */
    @Test
    @DisplayName("空内容不会通过任何类型的校验")
    void rejectsEmptyContent() {
        byte[] empty = new byte[0];
        assertThatThrownBy(() -> FileSignatures.verify(MediaFileType.PNG, empty))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> FileSignatures.verify(MediaFileType.MP4, empty))
                .isInstanceOf(IOException.class);
    }

    /**
     * \u6bcf\u4e2a\u5df2\u767b\u8bb0\u7684\u6269\u5c55\u540d\u90fd\u6709\u5bf9\u5e94\u7684\u7c7b\u578b\u4e0e MIME\uff0c\u4e0d\u5b58\u5728\u53ea\u767b\u8bb0\u4e0d\u6821\u9a8c\u7684\u6761\u76ee。
     * 关键处理步骤调用 <code>type.extension</code>、<code>type.mediaType</code>、<code>type.canonicalMimeType</code>、<code>MediaFileType.byExtension</code>。
     *
     * @param type 类型；类型为 <code>MediaFileType</code>
     */
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
