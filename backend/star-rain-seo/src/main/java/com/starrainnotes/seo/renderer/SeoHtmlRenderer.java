/*
 * SeoHtmlRenderer：公开页面与 SEO模块的内容渲染文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 将业务内容转换为展示结果；输入、输出和渲染所需协作对象在方法注释中说明。
 * 主要协作类型：SeoPageModel。
 * 阅读入口：render（渲染SEOHTML渲染器）；visit（visit处理入口）；visit（visit处理入口）；visit（visit处理入口）。
 */

package com.starrainnotes.seo.renderer;

import com.starrainnotes.seo.dto.SeoPageModel;
import java.net.URI;
import java.util.List;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Heading;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SEOHTML渲染器，属于公开页面与 SEO模块。
 * 将业务内容转换为展示结果；输入、输出和渲染所需协作对象在方法注释中说明。
 */
@Component
public class SeoHtmlRenderer {
    /**
     * 当前类使用的日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(SeoHtmlRenderer.class);
    /**
     * LENGTH。
     * 初始值为 <code>200000</code>。
     */
    private static final int MAX_MARKDOWN_LENGTH = 200_000;
    /**
     * EXTENSIONS。
     */
    private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());
    /**
     * parser。
     */
    private final Parser parser = Parser.builder().extensions(EXTENSIONS).build();
    /**
     * Markdown渲染器。
     */
    private final HtmlRenderer markdownRenderer = HtmlRenderer.builder().extensions(EXTENSIONS)
            .escapeHtml(true).sanitizeUrls(true).build();
    /**
     * 渲染SEOHTML渲染器。
     * 关键处理步骤调用 <code>escape</code>、<code>model.getTitle</code>、<code>model.getDescription</code>、<code>model.getCanonicalUrl</code>。
     *
     * @param model model；类型为 <code>SeoPageModel</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String render(SeoPageModel model) {
        String title = escape(model.getTitle());
        String description = escape(model.getDescription());
        String canonical = escape(model.getCanonicalUrl());
        String robots = escape(model.getRobotsDirective());
        String body = semanticBody(model.getBodyMarkdown());
        return "<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
            + "<title>" + title + "</title><meta name=\"description\" content=\"" + description + "\">"
            + "<meta name=\"robots\" content=\"" + robots + "\">"
            + "<link rel=\"canonical\" href=\"" + canonical + "\">"
            + "<meta property=\"og:title\" content=\"" + title + "\">"
            + "<meta property=\"og:description\" content=\"" + description + "\">"
            + "<meta property=\"og:url\" content=\"" + canonical + "\">"
            + (model.getCoverUrl() == null ? "" : "<meta property=\"og:image\" content=\"" + escape(model.getCoverUrl()) + "\">")
            + structuredData(model) + "</head><body>"
            + "<div id=\"app\"><main><article><h1>" + title + "</h1>"
            + "<p>" + description + "</p><div class=\"seo-body\">" + body
            + "</div></article></main></div></body></html>";
    }

    /**
     * structuredData处理入口。
     * 关键处理步骤调用 <code>model.getContentType</code>、<code>data.put</code>、<code>model.getTitle</code>、<code>model.getDescription</code>。
     *
     * @param model model；类型为 <code>SeoPageModel</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String structuredData(SeoPageModel model) {
        var data = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        String type = "BLOG".equals(model.getContentType()) ? "BlogPosting"
                : "CHAPTER".equals(model.getContentType()) ? "TechArticle" : "CreativeWork";
        data.put("@context", "https://schema.org").put("@type", type)
                .put("name", model.getTitle()).put("description", model.getDescription()).put("url", model.getCanonicalUrl());
        if (model.getCoverUrl() != null) { data.put("image", model.getCoverUrl()); }
        if (model.getUpdatedAt() != null) data.put("dateModified", model.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC).toString());
        String json = data.toString().replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
        return "<script type=\"application/ld+json\">" + json + "</script>";
    }

    /**
     * semantic正文处理入口。
     * 主要协作调用：<code>parser.parse</code>、<code>markdownRenderer.render</code>。
     *
     * @param markdown Markdown；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String semanticBody(String markdown) {
        if (markdown == null) return "";
        int end = Math.min(markdown.length(), MAX_MARKDOWN_LENGTH);
        if (end < markdown.length()) {
            if (Character.isHighSurrogate(markdown.charAt(end - 1))) end--;
            log.warn("SEO Markdown truncated from {} to {} characters", markdown.length(), end);
        }
        Node document = parser.parse(markdown.substring(0, end));
        document.accept(new AbstractVisitor()
    /**
     * 当前调用使用的匿名协作实现，属于公开页面与 SEO模块。
     * 将业务内容转换为展示结果；输入、输出和渲染所需协作对象在方法注释中说明。
     */
    {
            /**
             * visit处理入口。
             * 关键处理步骤调用 <code>heading.getLevel</code>、<code>heading.setLevel</code>、<code>visitChildren</code>。
             *
             * @param heading heading；类型为 <code>Heading</code>
             */
            @Override public void visit(Heading heading) {
                if (heading.getLevel() == 1) heading.setLevel(2);
                visitChildren(heading);
            }
            /**
             * visit处理入口。
             * 关键处理步骤调用 <code>safeUrl</code>、<code>link.getDestination</code>、<code>link.setDestination</code>、<code>visitChildren</code>。
             *
             * @param link 链接；类型为 <code>Link</code>
             */
            @Override public void visit(Link link) {
                if (!safeUrl(link.getDestination(), false)) link.setDestination("");
                visitChildren(link);
            }
            /**
             * visit处理入口。
             * 关键处理步骤调用 <code>safeUrl</code>、<code>image.getDestination</code>、<code>image.setDestination</code>、<code>visitChildren</code>。
             *
             * @param image 图片；类型为 <code>Image</code>
             */
            @Override public void visit(Image image) {
                if (!safeUrl(image.getDestination(), true)) image.setDestination("");
                visitChildren(image);
            }
        });
        return markdownRenderer.render(document);
    }

    /**
     * 安全地址处理入口。
     * 关键处理步骤调用 <code>destination.isBlank</code>、<code>destination.startsWith</code>、<code>destination.indexOf</code>、<code>destination.chars</code>。
     *
     * @param destination destination；类型为 <code>String</code>
     * @param image 图片；类型为 <code>boolean</code>
     * @return 本方法条件是否成立的判断结果
     */
    private boolean safeUrl(String destination, boolean image) {
        if (destination == null || destination.isBlank() || destination.startsWith("//")
                || destination.indexOf('\\') >= 0 || destination.chars().anyMatch(Character::isISOControl)) return false;
        try {
            URI uri = URI.create(destination);
            if (uri.getUserInfo() != null) return false;
            String scheme = uri.getScheme();
            if (scheme == null) return !image || !destination.startsWith("#");
            return "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)
                    || (!image && "mailto".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    /**
     * 转义SEOHTML渲染器。
     * 关键处理步骤调用 <code>text.replace</code>。
     *
     * @param text 文本；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
