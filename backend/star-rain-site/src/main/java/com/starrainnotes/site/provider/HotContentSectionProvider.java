/*
 * HotContentSectionProvider：站点模块的模块支持文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：AnalyticsQueryApi、BlogReferenceApi、BlogPostSummary、PortfolioReferenceApi、PortfolioPublishedWork、SitePublicConfigVO、HotContentItemVO。
 * 阅读入口：sectionCode（区块代码处理入口）；load（加载Hot内容区块提供方）。
 */

package com.starrainnotes.site.provider;

import com.starrainnotes.analytics.api.AnalyticsQueryApi;
import com.starrainnotes.blog.api.BlogReferenceApi;
import com.starrainnotes.blog.api.dto.BlogPostSummary;
import com.starrainnotes.portfolio.api.PortfolioReferenceApi;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;
import com.starrainnotes.site.api.vo.SitePublicConfigVO;
import com.starrainnotes.site.vo.HotContentItemVO;
import com.starrainnotes.site.entity.HomeSectionEntity;
import com.starrainnotes.tutorial.content.api.PublishedTutorial;
import com.starrainnotes.tutorial.content.api.TutorialReferenceApi;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Hot内容区块提供方，属于站点模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class HotContentSectionProvider implements HomeSectionProvider {
    /**
     * 统计。
     * 协作类型：<code>AnalyticsQueryApi</code>。
     */
    private final AnalyticsQueryApi analytics;
    /**
     * blogs。
     * 协作类型：<code>BlogReferenceApi</code>。
     */
    private final BlogReferenceApi blogs;
    /**
     * tutorials。
     * 协作类型：<code>TutorialReferenceApi</code>。
     */
    private final TutorialReferenceApi tutorials;
    /**
     * 作品列表。
     * 协作类型：<code>PortfolioReferenceApi</code>。
     */
    private final PortfolioReferenceApi works;
    /**
     * limits。
     */
    private final HomeSectionDisplayOptions limits;
    /**
     * 区块代码处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override public String sectionCode() { return "HOT_CONTENT"; }
    /**
     * 加载Hot内容区块提供方。
     * 主要协作调用：<code>analytics.hotContent</code>、<code>limits.limitOf</code>。
     *
     * @param section 区块；类型为 <code>HomeSectionEntity</code>
     * @param config 配置；类型为 <code>SitePublicConfigVO</code>
     * @return 类型为 <code>Object</code> 的处理结果
     */
    @Override public Object load(HomeSectionEntity section, SitePublicConfigVO config) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return analytics.hotContent(null, today.minusDays(29), today, limits.limitOf(section) * 2).stream()
                .map(this::publicItem).filter(java.util.Objects::nonNull)
                .limit(limits.limitOf(section)).toList();
    }

    /**
     * 公开Item处理入口。
     * 主要协作调用：<code>blogs.summary</code>、<code>blogs.isPublished</code>、<code>tutorials.publishedTutorial</code>、<code>works.publishedWork</code>。
     *
     * @param row 行；类型为 <code>Map&lt;String, Object&gt;</code>
     * @return 类型为 <code>HotContentItemVO</code> 的处理结果
     */
    private HotContentItemVO publicItem(Map<String, Object> row) {
        String type = String.valueOf(row.get("contentType"));
        Long id = ((Number) row.get("contentId")).longValue();
        long count = ((Number) row.get("viewCount")).longValue();
        if ("BLOG".equals(type)) {
            BlogPostSummary post = blogs.summary(id);
            return post == null || !blogs.isPublished(id) ? null
                    : new HotContentItemVO(type, post.getTitle(), "/blog/posts/" + post.getSlug(), count);
        }
        if ("TUTORIAL".equals(type)) {
            PublishedTutorial tutorial = tutorials.publishedTutorial(id).orElse(null);
            return tutorial == null ? null
                    : new HotContentItemVO(type, tutorial.getTitle(), "/tutorials/" + tutorial.getSlug(), count);
        }
        if ("PORTFOLIO".equals(type)) {
            PortfolioPublishedWork work = works.publishedWork(id).orElse(null);
            return work == null ? null
                    : new HotContentItemVO(type, work.getTitle(), "/portfolio/" + work.getSlug(), count);
        }
        return null;
    }
}
