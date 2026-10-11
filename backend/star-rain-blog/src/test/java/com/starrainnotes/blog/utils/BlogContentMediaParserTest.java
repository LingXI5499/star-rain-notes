package com.starrainnotes.blog.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/*
 * 正文媒体引用解析测试。
 *
 * 这是 blog.content 引用的唯一来源，解析错了会导致 Media 侧归档保护失效：
 * 引用少解析一条，作者就能归档一张还在正文里用的图。
 */
class BlogContentMediaParserTest {

    @Test
    @DisplayName("只识别 Media 模块的公开内容地址，并保持出现顺序")
    void extractsContentUrlsInOrder() {
        String body = """
                # 标题

                ![封面](/api/media/assets/12/content)

                正文中间夹一段

                <img src="/api/media/assets/7/content" />
                """;

        assertThat(BlogContentMediaParser.extractMediaAssetIds(body)).containsExactly(12L, 7L);
    }

    @Test
    @DisplayName("同一媒体重复出现只登记一条引用")
    void deduplicatesRepeatedAssets() {
        String body = "![](/api/media/assets/5/content)\n![](/api/media/assets/5/content)";

        assertThat(BlogContentMediaParser.extractMediaAssetIds(body)).containsExactly(5L);
    }

    @Test
    @DisplayName("外部图床地址不构成对本站媒体的引用")
    void ignoresExternalUrls() {
        String body = "![](https://cdn.example.com/api/media/assets/9/content.png)\n![](https://img.example.com/a.png)";

        assertThat(BlogContentMediaParser.extractMediaAssetIds(body)).isEmpty();
    }

    @Test
    @DisplayName("空正文与 null 都返回空集合，不抛异常")
    void handlesEmptyBody() {
        assertThat(BlogContentMediaParser.extractMediaAssetIds(null)).isEmpty();
        assertThat(BlogContentMediaParser.extractMediaAssetIds("")).isEmpty();
        assertThat(BlogContentMediaParser.extractMediaAssetIds("   ")).isEmpty();
    }

    @Test
    @DisplayName("超出 long 范围的 ID 被忽略，不让整篇保存失败")
    void ignoresOverflowingIds() {
        String body = "![](/api/media/assets/99999999999999999999999/content)\n![](/api/media/assets/3/content)";

        assertThat(BlogContentMediaParser.extractMediaAssetIds(body)).containsExactly(3L);
    }

    @Test
    @DisplayName("接口返回的集合不可变，调用方无法在别处篡改引用集合")
    void returnsIndependentList() {
        List<Long> first = BlogContentMediaParser.extractMediaAssetIds("![](/api/media/assets/1/content)");
        List<Long> second = BlogContentMediaParser.extractMediaAssetIds("![](/api/media/assets/2/content)");

        assertThat(first).containsExactly(1L);
        assertThat(second).containsExactly(2L);
    }
}
