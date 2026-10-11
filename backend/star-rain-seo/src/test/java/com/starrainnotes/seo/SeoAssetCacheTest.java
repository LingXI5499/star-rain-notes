/*
 * SeoAssetCacheTest：公开页面与 SEO模块的测试文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：SeoProperties、SeoInteractiveAssetRenderer。
 * 阅读入口：reusesImmutableEntryRefreshesChangedBuildAndAvoidsDuplicates（reusesImmutable入口RefreshesChangedBuild及AvoidsDuplicates处理入口）。
 */

package com.starrainnotes.seo;

import static org.junit.jupiter.api.Assertions.*;
import com.starrainnotes.seo.config.SeoProperties;
import com.starrainnotes.seo.renderer.SeoInteractiveAssetRenderer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * SEO资产缓存测试，属于公开页面与 SEO模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class SeoAssetCacheTest {
    /**
     * directory。
     */
    @TempDir Path directory;
    /**
     * HTML。
     */
    private static final String HTML = "<html><head></head><body><h1>Semantic content</h1></body></html>";
    /**
     * reusesImmutable入口RefreshesChangedBuild及AvoidsDuplicates处理入口。
     * 主要协作调用：<code>directory.resolve</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void reusesImmutableEntryRefreshesChangedBuildAndAvoidsDuplicates() throws Exception {
        Path index = directory.resolve("index.html");
        Files.writeString(index, "<script type=\"module\" src=\"/assets/old.js\"></script><link rel=\"stylesheet\" href=\"/assets/old.css\">");
        SeoProperties properties = new SeoProperties(); properties.setFrontendIndexPath(index.toString());
        var renderer = new SeoInteractiveAssetRenderer(properties);
        String first = renderer.include(HTML);
        assertTrue(first.contains("/assets/old.js"));
        assertEquals(first, renderer.include(first));
        // A same-version file edit proves repeated requests reuse the parsed cache.
        FileTime modified = Files.getLastModifiedTime(index);
        Files.writeString(index, Files.readString(index).replace("old", "new"));
        Files.setLastModifiedTime(index, modified);
        assertEquals(first, renderer.include(HTML));
        Files.setLastModifiedTime(index, FileTime.fromMillis(modified.toMillis() + 10_000));
        try (var pool = Executors.newFixedThreadPool(8)) {
            var tasks = java.util.stream.IntStream.range(0, 32)
                    .mapToObj(i -> pool.submit(() -> renderer.include(HTML))).toList();
            for (var task : tasks) assertTrue(task.get().contains("/assets/new.js"));
        }
        Files.delete(index);
        assertEquals(HTML, renderer.include(HTML));
        assertEquals(HTML, renderer.include(HTML));
    }
}
