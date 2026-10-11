/*
 * SitemapServiceImpl：公开页面与 SEO模块的业务实现文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：SeoPageMapper、SeoPageSnapshot、SitemapService。
 * 阅读入口：xml（xml处理入口）。
 */

package com.starrainnotes.seo.service.impl;

import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.api.dto.SeoPageSnapshot;
import com.starrainnotes.seo.service.SitemapService;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Sitemap业务服务实现，属于公开页面与 SEO模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class SitemapServiceImpl implements SitemapService {
    /**
     * 数据映射。
     * 协作类型：<code>SeoPageMapper</code>。
     */
    private final SeoPageMapper mapper;

    /**
     * xml处理入口。
     * 主要协作调用：<code>mapper.activePages</code>。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    public String xml() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        for (SeoPageSnapshot page : mapper.activePages()) {
            xml.append("<url><loc>").append(escape(page.getCanonicalUrl())).append("</loc>");
            // sourceVersionRef already persists the source timestamp; rebuilding is not an edit.
            if (page.getSourceVersionRef() != null) {
                try {
                    var modified = LocalDateTime.parse(page.getSourceVersionRef()).atOffset(ZoneOffset.UTC);
                    xml.append("<lastmod>").append(modified)
                        .append("</lastmod>");
                } catch (DateTimeParseException opaqueRevision) {
                    // Sources without a trustworthy date omit lastmod.
                }
            }
            xml.append("</url>");
        }
        return xml.append("</urlset>").toString();
    }

    /**
     * 转义Sitemap。
     * 关键处理步骤调用 <code>value.replace</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
