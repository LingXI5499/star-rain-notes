package com.starrainnotes.media.enumeration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/*
 * 扩展名登记表与声明 Content-Type 的接受规则测试。
 *
 * 这里的边界很重要：声明类型只是辅助检查，真实内容校验才是权威。
 * 放行「未声明具体类型」是为了兼容命令行与脚本客户端，
 * 但不能因此放过「声明了具体类型却与扩展名不符」的情况。
 */
class MediaFileTypeTest {

    @Test
    @DisplayName("未声明具体类型（空值 / octet-stream）交给真实内容校验判定")
    void toleratesUndeclaredContentType() {
        for (MediaFileType type : List.of(MediaFileType.BMP, MediaFileType.WEBP,
                MediaFileType.MP3, MediaFileType.WAV, MediaFileType.MP4, MediaFileType.MKV)) {
            assertThat(type.acceptsDeclaredContentType(null)).as("%s null", type).isTrue();
            assertThat(type.acceptsDeclaredContentType("")).as("%s blank", type).isTrue();
            assertThat(type.acceptsDeclaredContentType("application/octet-stream"))
                    .as("%s octet-stream", type).isTrue();
        }
    }

    @Test
    @DisplayName("声明了具体类型就必须与扩展名一致")
    void rejectsMismatchedDeclaredContentType() {
        assertThat(MediaFileType.PNG.acceptsDeclaredContentType("image/png")).isTrue();
        assertThat(MediaFileType.PNG.acceptsDeclaredContentType("application/pdf")).isFalse();
        assertThat(MediaFileType.MP4.acceptsDeclaredContentType("video/mp4")).isTrue();
        assertThat(MediaFileType.MP4.acceptsDeclaredContentType("image/png")).isFalse();
        assertThat(MediaFileType.DOCX.acceptsDeclaredContentType("application/zip")).isTrue();
    }

    @Test
    @DisplayName("Content-Type 参数与大小写被规范化后再比较")
    void normalizesContentType() {
        assertThat(MediaFileType.PNG.acceptsDeclaredContentType("IMAGE/PNG")).isTrue();
        assertThat(MediaFileType.TXT.acceptsDeclaredContentType("text/plain; charset=utf-8")).isTrue();
        assertThat(MediaFileType.MD.acceptsDeclaredContentType("text/markdown; charset=UTF-8")).isTrue();
    }

    @Test
    @DisplayName("扩展名解析大小写不敏感，没有扩展名返回空串")
    void extensionOf() {
        assertThat(MediaFileType.extensionOf("Cover.PNG")).isEqualTo("png");
        assertThat(MediaFileType.extensionOf("文档.MD")).isEqualTo("md");
        assertThat(MediaFileType.extensionOf("README")).isEmpty();
        assertThat(MediaFileType.extensionOf("trailing.")).isEmpty();
        assertThat(MediaFileType.extensionOf(null)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(MediaFileType.class)
    @DisplayName("只有图片需要整份读入（解析宽高）；其余类型流式落盘")
    void onlyImagesRequireFullContent(MediaFileType type) {
        assertThat(type.requiresFullContent())
                .as("%s", type)
                .isEqualTo(type.mediaType() == MediaType.IMAGE);
    }
}
