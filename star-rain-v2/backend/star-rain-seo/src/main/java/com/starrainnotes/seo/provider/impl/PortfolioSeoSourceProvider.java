package com.starrainnotes.seo.provider.impl;

import com.starrainnotes.seo.provider.SeoSourceProvider;

import com.starrainnotes.seo.dto.SeoSourceDocument;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.api.PortfolioPublicApi;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortfolioSeoSourceProvider implements SeoSourceProvider {
    private final PortfolioPublicApi works;

    @Override
    public boolean supports(String path) { return path != null && path.matches("/portfolio/[a-zA-Z0-9_-]{1,180}"); }

    @Override
    public Optional<SeoSourceDocument> loadByRoute(String path) {
        if (!supports(path)) return Optional.empty();
        return works.publishedWorkBySlug(path.substring("/portfolio/".length())).map(this::document);
    }

    @Override
    public List<SeoSourceDocument> listPublished() {
        List<SeoSourceDocument> result = new ArrayList<>();
        for (int page = 1; ; page++) {
            PageResult<PortfolioPublishedWork> rows = works.publishedWorks(page, 100);
            rows.getItems().forEach(item -> result.add(document(item)));
            if ((long) page * 100 >= rows.getTotal()) break;
        }
        return result;
    }

    private SeoSourceDocument document(PortfolioPublishedWork work) {
        return SeoSourceDocument.builder().routePath("/portfolio/" + work.getSlug())
            .contentType("PORTFOLIO").contentId(work.getId())
            .title(work.getSeoTitle() == null || work.getSeoTitle().isBlank() ? work.getTitle() : work.getSeoTitle())
            .summary(work.getSeoDescription() == null || work.getSeoDescription().isBlank() ? work.getSummary() : work.getSeoDescription()).bodyMarkdown(work.getBodyMarkdown())
            .coverUrl(work.getCoverUrl()).updatedAt(work.getUpdatedAt()).build();
    }
}
