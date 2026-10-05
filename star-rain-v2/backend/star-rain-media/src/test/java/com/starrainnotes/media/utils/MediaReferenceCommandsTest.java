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
class MediaReferenceCommandsTest {

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

    @Test
    @DisplayName("usageCode 前缀与 sourceModule 不一致时被拒绝")
    void rejectsUsageCodeFromAnotherModule() {
        assertThatThrownBy(() -> MediaReferenceCommands.normalize(
                new MediaReferenceCommand(1L, "TUTORIAL", "CHAPTER", 2L, "blog.cover")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("前缀");
    }

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
