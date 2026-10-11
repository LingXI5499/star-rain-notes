/*
 * MediaFileTypeTest：媒体模块的测试文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 阅读入口：toleratesUndeclaredContentType（\u672a\u58f0\u660e\u5177\u4f53\u7c7b\u578b\uff08\u7a7a\u503c / octet-stream\uff09\u4ea4\u7ed9\u771f\u5b9e\u5185\u5bb9\u6821\u9a8c\u5224\u5b9a）；rejectsMismatchedDeclaredContentType（\u58f0\u660e\u4e86\u5177\u4f53\u7c7b\u578b\u5c31\u5fc5\u987b\u4e0e\u6269\u5c55\u540d\u4e00\u81f4）；normalizesContentType（Content-Type \u53c2\u6570\u4e0e\u5927\u5c0f\u5199\u88ab\u89c4\u8303\u5316\u540e\u518d\u6bd4\u8f83）；extensionOf（\u6269\u5c55\u540d\u89e3\u6790\u5927\u5c0f\u5199\u4e0d\u654f\u611f\uff0c\u6ca1\u6709\u6269\u5c55\u540d\u8fd4\u56de\u7a7a\u4e32）。
 */

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
/**
 * 媒体文件类型测试，属于媒体模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class MediaFileTypeTest {

    /**
     * \u672a\u58f0\u660e\u5177\u4f53\u7c7b\u578b\uff08\u7a7a\u503c / octet-stream\uff09\u4ea4\u7ed9\u771f\u5b9e\u5185\u5bb9\u6821\u9a8c\u5224\u5b9a。
     * 关键处理步骤调用 <code>List.of</code>、<code>type.acceptsDeclaredContentType</code>。
     */
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

    /**
     * \u58f0\u660e\u4e86\u5177\u4f53\u7c7b\u578b\u5c31\u5fc5\u987b\u4e0e\u6269\u5c55\u540d\u4e00\u81f4。
     * 关键处理步骤调用 <code>MediaFileType.PNG.acceptsDeclaredContentType</code>、<code>MediaFileType.MP4.acceptsDeclaredContentType</code>、<code>MediaFileType.DOCX.acceptsDeclaredContentType</code>。
     */
    @Test
    @DisplayName("声明了具体类型就必须与扩展名一致")
    void rejectsMismatchedDeclaredContentType() {
        assertThat(MediaFileType.PNG.acceptsDeclaredContentType("image/png")).isTrue();
        assertThat(MediaFileType.PNG.acceptsDeclaredContentType("application/pdf")).isFalse();
        assertThat(MediaFileType.MP4.acceptsDeclaredContentType("video/mp4")).isTrue();
        assertThat(MediaFileType.MP4.acceptsDeclaredContentType("image/png")).isFalse();
        assertThat(MediaFileType.DOCX.acceptsDeclaredContentType("application/zip")).isTrue();
    }

    /**
     * Content-Type \u53c2\u6570\u4e0e\u5927\u5c0f\u5199\u88ab\u89c4\u8303\u5316\u540e\u518d\u6bd4\u8f83。
     * 关键处理步骤调用 <code>MediaFileType.PNG.acceptsDeclaredContentType</code>、<code>MediaFileType.TXT.acceptsDeclaredContentType</code>、<code>MediaFileType.MD.acceptsDeclaredContentType</code>。
     */
    @Test
    @DisplayName("Content-Type 参数与大小写被规范化后再比较")
    void normalizesContentType() {
        assertThat(MediaFileType.PNG.acceptsDeclaredContentType("IMAGE/PNG")).isTrue();
        assertThat(MediaFileType.TXT.acceptsDeclaredContentType("text/plain; charset=utf-8")).isTrue();
        assertThat(MediaFileType.MD.acceptsDeclaredContentType("text/markdown; charset=UTF-8")).isTrue();
    }

    /**
     * \u6269\u5c55\u540d\u89e3\u6790\u5927\u5c0f\u5199\u4e0d\u654f\u611f\uff0c\u6ca1\u6709\u6269\u5c55\u540d\u8fd4\u56de\u7a7a\u4e32。
     * 关键处理步骤调用 <code>MediaFileType.extensionOf</code>。
     */
    @Test
    @DisplayName("扩展名解析大小写不敏感，没有扩展名返回空串")
    void extensionOf() {
        assertThat(MediaFileType.extensionOf("Cover.PNG")).isEqualTo("png");
        assertThat(MediaFileType.extensionOf("文档.MD")).isEqualTo("md");
        assertThat(MediaFileType.extensionOf("README")).isEmpty();
        assertThat(MediaFileType.extensionOf("trailing.")).isEmpty();
        assertThat(MediaFileType.extensionOf(null)).isEmpty();
    }

    /**
     * \u53ea\u6709\u56fe\u7247\u9700\u8981\u6574\u4efd\u8bfb\u5165\uff08\u89e3\u6790\u5bbd\u9ad8\uff09\uff1b\u5176\u4f59\u7c7b\u578b\u6d41\u5f0f\u843d\u76d8。
     * 关键处理步骤调用 <code>type.requiresFullContent</code>、<code>type.mediaType</code>。
     *
     * @param type 类型；类型为 <code>MediaFileType</code>
     */
    @ParameterizedTest
    @EnumSource(MediaFileType.class)
    @DisplayName("只有图片需要整份读入（解析宽高）；其余类型流式落盘")
    void onlyImagesRequireFullContent(MediaFileType type) {
        assertThat(type.requiresFullContent())
                .as("%s", type)
                .isEqualTo(type.mediaType() == MediaType.IMAGE);
    }
}
