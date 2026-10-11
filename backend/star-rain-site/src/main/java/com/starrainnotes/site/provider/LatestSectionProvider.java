package com.starrainnotes.site.provider;

import com.starrainnotes.blog.api.BlogPublicApi;
import com.starrainnotes.blog.api.dto.BlogPostSummary;
import com.starrainnotes.portfolio.api.PortfolioPublicApi;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;
import com.starrainnotes.site.api.vo.SitePublicConfigVO;
import com.starrainnotes.site.vo.HomeLatestItemVO;
import com.starrainnotes.site.entity.HomeSectionEntity;
import com.starrainnotes.tutorial.content.api.PublishedTutorial;
import com.starrainnotes.tutorial.content.api.TutorialPublicApi;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/*
 * 首页「最近更新」区块：把教程、博客、作品三种内容按发布时间倒序合成一条列表。
 *
 * 三处细节值得说明：
 *   1. 每个来源单独 try/catch。聚合器只在 provider **抛出**时把整个区块降级，
 *      如果这里不隔离，博客库抖一下首页的教程更新也会一起消失。
 *   2. 三个来源各取 `limit` 条再合并排序后截断，结果与「全局取前 limit 条」等价：
 *      全局前 N 条里任一类型至多占 N 条，而该类型的前 N 条必然包含自己那部分。
 *   3. 作品的排序键是「最后更新时间」：作品正文会反复修订，用发布时间会把长期维护的作品压到列表底部。
 */
@Component
@RequiredArgsConstructor
public class LatestSectionProvider implements HomeSectionProvider {
    private static final Logger log = LoggerFactory.getLogger(LatestSectionProvider.class);
    private final TutorialPublicApi tutorials;
    private final BlogPublicApi blogs;
    private final PortfolioPublicApi works;
    private final HomeSectionDisplayOptions limits;

    @Override
    public String sectionCode() {
        return "LATEST";
    }

    @Override
    public Object load(HomeSectionEntity section, SitePublicConfigVO config) {
        int limit = limits.limitOf(section);
        List<HomeLatestItemVO> items = new ArrayList<>();
        items.addAll(from("tutorial", () -> tutorials.latestPublished(limit).stream()
                .map(this::tutorial).toList()));
        items.addAll(from("blog", () -> blogs.latestPublished(limit).stream()
                .map(this::blog).toList()));
        items.addAll(from("portfolio", () -> works.publishedWorks(1, limit).getItems().stream()
                .map(this::work).toList()));
        return items.stream()
                .sorted(Comparator.comparing(HomeLatestItemVO::getPublishedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private List<HomeLatestItemVO> from(String source, Supplier<List<HomeLatestItemVO>> reader) {
        try {
            return reader.get();
        } catch (RuntimeException exception) {
            log.warn("Home latest section source {} unavailable", source, exception);
            return List.of();
        }
    }

    private HomeLatestItemVO tutorial(PublishedTutorial item) {
        return HomeLatestItemVO.builder().type("TUTORIAL").id(item.getId()).title(item.getTitle())
                .summary(item.getSummary()).routePath("/tutorials/" + item.getSlug())
                .publishedAt(item.getPublishedAt()).build();
    }

    private HomeLatestItemVO blog(BlogPostSummary item) {
        return HomeLatestItemVO.builder().type("BLOG").id(item.getId()).title(item.getTitle())
                .summary(item.getSummary()).routePath("/blog/posts/" + item.getSlug())
                .publishedAt(item.getPublishedAt()).build();
    }

    private HomeLatestItemVO work(PortfolioPublishedWork item) {
        LocalDateTime activity = item.getUpdatedAt() == null ? item.getPublishedAt() : item.getUpdatedAt();
        return HomeLatestItemVO.builder().type("PORTFOLIO").id(item.getId()).title(item.getTitle())
                .summary(item.getSummary()).routePath("/portfolio/" + item.getSlug())
                .publishedAt(activity).build();
    }
}
