/*
 * MediaReferenceCommandsTest：媒体模块的测试文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：MediaReferenceCommands、ApiException、MediaUsageCodes、MediaReferenceCommand。
 * 阅读入口：normalizesValidCommand（\u5408\u6cd5\u5f15\u7528\u88ab\u89c4\u8303\u5316\uff1a\u6a21\u5757\u4e0e\u7c7b\u578b\u8f6c\u5927\u5199\uff0cusageCode \u4fdd\u6301\u5c0f\u5199）；rejectsUsageCodeFromAnotherModule（usageCode \u524d\u7f00\u4e0e sourceModule \u4e0d\u4e00\u81f4\u65f6\u88ab\u62d2\u7edd）；rejectsMalformedUsageCode（usageCode \u683c\u5f0f\u975e\u6cd5\u65f6\u88ab\u62d2\u7edd）；rejectsNonPositiveIds（ID \u5fc5\u987b\u4e3a\u6b63\u6574\u6570\uff0cnull \u547d\u4ee4\u88ab\u62d2\u7edd）。
 */

package com.starrainnotes.media.utils;

import com.starrainnotes.media.utils.MediaReferenceCommands;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.api.constant.MediaUsageCodes;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/*
 * 媒体引用参数校验测试。
 * 这是“浏览器不能伪造跨模块引用”的第一道防线，必须严格。
 */
/**
 * 媒体引用Commands测试，属于媒体模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class MediaReferenceCommandsTest {

    /**
     * \u5408\u6cd5\u5f15\u7528\u88ab\u89c4\u8303\u5316\uff1a\u6a21\u5757\u4e0e\u7c7b\u578b\u8f6c\u5927\u5199\uff0cusageCode \u4fdd\u6301\u5c0f\u5199。
     * 关键处理步骤调用 <code>MediaReferenceCommands.normalize</code>、<code>normalized.getSourceModule</code>、<code>normalized.getSourceType</code>、<code>normalized.getUsageCode</code>。
     */
    @Test
    @DisplayName("合法引用被规范化：模块与类型转大写，usageCode 保持小写")
    void normalizesValidCommand() {
        MediaReferenceCommand normalized = MediaReferenceCommands.normalize(
                new MediaReferenceCommand(1024L, " blog ", "post", 57L, "blog.cover"));

        assertThat(normalized.getSourceModule()).isEqualTo("BLOG");
        assertThat(normalized.getSourceType()).isEqualTo("POST");
        assertThat(normalized.getUsageCode()).isEqualTo("blog.cover");
        assertThat(normalized.getMediaAssetId()).isEqualTo(1024L);
        assertThat(normalized.getSourceId()).isEqualTo(57L);
    }

    /**
     * usageCode \u524d\u7f00\u4e0e sourceModule \u4e0d\u4e00\u81f4\u65f6\u88ab\u62d2\u7edd。
     * 关键处理步骤调用 <code>MediaReferenceCommands.normalize</code>。
     */
    @Test
    @DisplayName("usageCode 前缀与 sourceModule 不一致时被拒绝")
    void rejectsUsageCodeFromAnotherModule() {
        assertThatThrownBy(() -> MediaReferenceCommands.normalize(
                new MediaReferenceCommand(1L, "TUTORIAL", "CHAPTER", 2L, "blog.cover")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("前缀");
    }

    /**
     * usageCode \u683c\u5f0f\u975e\u6cd5\u65f6\u88ab\u62d2\u7edd。
     * 关键处理步骤调用 <code>MediaReferenceCommands.normalize</code>。
     */
    @Test
    @DisplayName("usageCode 格式非法时被拒绝")
    void rejectsMalformedUsageCode() {
        for (String usageCode : new String[]{"blog", "blog.", ".cover", "Blog.Cover", "blog cov er"}) {
            assertThatThrownBy(() -> MediaReferenceCommands.normalize(
                    new MediaReferenceCommand(1L, "BLOG", "POST", 2L, usageCode)))
                    .as("usageCode=%s", usageCode)
                    .isInstanceOf(ApiException.class)
                    .extracting(ex -> ((ApiException) ex).getCode())
                    .isEqualTo("MEDIA_REFERENCE_INVALID");
        }
    }

    /**
     * ID \u5fc5\u987b\u4e3a\u6b63\u6574\u6570\uff0cnull \u547d\u4ee4\u88ab\u62d2\u7edd。
     * 关键处理步骤调用 <code>MediaReferenceCommands.normalize</code>。
     */
    @Test
    @DisplayName("ID 必须为正整数，null 命令被拒绝")
    void rejectsNonPositiveIds() {
        assertThatThrownBy(() -> MediaReferenceCommands.normalize(
                new MediaReferenceCommand(0L, "BLOG", "POST", 2L, "blog.cover")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> MediaReferenceCommands.normalize(
                new MediaReferenceCommand(1L, "BLOG", "POST", null, "blog.cover")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> MediaReferenceCommands.normalize(null))
                .isInstanceOf(ApiException.class);
    }

    /**
     * \u6765\u6e90\u4e09\u5143\u7ec4\u6821\u9a8c\u4e0e attach \u4fdd\u6301\u4e00\u81f4。
     * 关键处理步骤调用 <code>MediaReferenceCommands.normalizeSource</code>。
     */
    @Test
    @DisplayName("来源三元组校验与 attach 保持一致")
    void normalizesSource() {
        assertThat(MediaReferenceCommands.normalizeSource("tutorial", "chapter", 9L))
                .containsExactly("TUTORIAL", "CHAPTER");
        assertThatThrownBy(() -> MediaReferenceCommands.normalizeSource("TUTORIAL", "CHAPTER", null))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> MediaReferenceCommands.normalizeSource("  ", "CHAPTER", 1L))
                .isInstanceOf(ApiException.class);
    }

    /**
     * \u5df2\u767b\u8bb0\u7684 usageCode \u90fd\u6ee1\u8db3\u683c\u5f0f\u4e0e\u524d\u7f00\u7ea6\u675f。
     * 关键处理步骤调用 <code>MediaUsageCodes.hasValidFormat</code>、<code>MediaUsageCodes.matchesSourceModule</code>。
     */
    @Test
    @DisplayName("已登记的 usageCode 都满足格式与前缀约束")
    void registeredUsageCodesAreConsistent() {
        assertThat(MediaUsageCodes.hasValidFormat(MediaUsageCodes.BLOG_COVER)).isTrue();
        assertThat(MediaUsageCodes.matchesSourceModule(MediaUsageCodes.BLOG_COVER, "BLOG")).isTrue();
        assertThat(MediaUsageCodes.matchesSourceModule(MediaUsageCodes.BLOG_COVER, "TUTORIAL")).isFalse();

        assertThat(MediaUsageCodes.matchesSourceModule(
                MediaUsageCodes.TUTORIAL_CHAPTER_CONTENT, "TUTORIAL")).isTrue();
        assertThat(MediaUsageCodes.matchesSourceModule(
                MediaUsageCodes.PORTFOLIO_SCREENSHOT, "PORTFOLIO")).isTrue();
        assertThat(MediaUsageCodes.matchesSourceModule(
                MediaUsageCodes.PROFILE_AVATAR, "PROFILE")).isTrue();
        assertThat(MediaUsageCodes.matchesSourceModule(MediaUsageCodes.SITE_LOGO, "SITE")).isTrue();
    }
}
