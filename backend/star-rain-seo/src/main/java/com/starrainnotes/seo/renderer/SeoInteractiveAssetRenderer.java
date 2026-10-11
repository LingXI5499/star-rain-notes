/*
 * SeoInteractiveAssetRenderer：公开页面与 SEO模块的内容渲染文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 将业务内容转换为展示结果；输入、输出和渲染所需协作对象在方法注释中说明。
 * 主要协作类型：SeoProperties。
 * 阅读入口：include（include处理入口）。
 */

package com.starrainnotes.seo.renderer;

import com.starrainnotes.seo.config.SeoProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SEOInteractive资产渲染器，属于公开页面与 SEO模块。
 * 将业务内容转换为展示结果；输入、输出和渲染所需协作对象在方法注释中说明。
 */
@Component
@RequiredArgsConstructor
public class SeoInteractiveAssetRenderer {
    /**
     * 当前类使用的日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(SeoInteractiveAssetRenderer.class);
    /**
     * 资产。
     */
    private static final Pattern ASSET = Pattern.compile(
        "(?is)<script\\b[^>]*\\bsrc=\"/assets/[^\"]+\"[^>]*></script>|<link\\b[^>]*\\bhref=\"/assets/[^\"]+\"[^>]*>");
    /**
     * 本模块的外部配置属性。
     * 协作类型：<code>SeoProperties</code>。
     */
    private final SeoProperties properties;
    /**
     * cached。
     */
    private volatile AssetEntry cached;

    /**
     * include处理入口。
     * 主要协作调用：<code>properties.getFrontendIndexPath</code>。
     *
     * @param html HTML；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String include(String html) {
        String path = properties.getFrontendIndexPath();
        if (path == null || path.isBlank()) return html;
        StringBuilder assets = new StringBuilder();
        for (String tag : assetTags(path)) {
            if (!html.contains(tag)) assets.append(tag);
        }
        return html.replace("</head>", assets + "</head>");
    }

    /**
     * 资产标签列表处理入口。
     * 主要协作调用：<code>cached.version.equals</code>、<code>ASSET.matcher</code>。
     * 实现中的显式失败类型包括 <code>IOException</code>；具体触发条件见对应分支。
     *
     * @param configuredPath configured路径；类型为 <code>String</code>
     * @return 本次处理得到的条目集合
     */
    private synchronized List<String> assetTags(String configuredPath) {
        Path path = Path.of(configuredPath).toAbsolutePath().normalize();
        String version = path + ":missing";
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            version = path + ":" + attributes.lastModifiedTime() + ":" + attributes.size() + ":" + attributes.fileKey();
            if (cached != null && cached.version.equals(version)) return cached.tags;
            if (attributes.size() > 1_048_576) throw new IOException("Frontend index exceeds size limit");
            String index = Files.readString(path, StandardCharsets.UTF_8);
            Matcher matcher = ASSET.matcher(index);
            var tags = new LinkedHashSet<String>();
            while (matcher.find()) tags.add(matcher.group());
            cached = new AssetEntry(version, List.copyOf(tags));
        } catch (IOException exception) {
            if (cached == null || !cached.version.equals(version)) {
                log.warn("Frontend build index unavailable for SEO HTML; serving semantic snapshot");
                cached = new AssetEntry(version, List.of());
            }
        }
        return cached.tags;
    }

    /**
     * 资产入口，属于公开页面与 SEO模块。
     * 将业务内容转换为展示结果；输入、输出和渲染所需协作对象在方法注释中说明。
     */
    private static final class AssetEntry {
        /**
         * 版本。
         */
        private final String version;
        /**
         * 标签列表。
         */
        private final List<String> tags;
        /**
         * 构造AssetEntry，接收本类使用的依赖或初始化参数。
         *
         * @param version 版本；类型为 <code>String</code>
         * @param tags 标签列表；类型为 <code>List&lt;String&gt;</code>
         */
        private AssetEntry(String version, List<String> tags) {
            this.version = version;
            this.tags = tags;
        }
    }
}
