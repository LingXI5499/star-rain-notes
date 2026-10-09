package com.starrainnotes.portfolio.api.impl;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.api.PortfolioPublicApi;
import com.starrainnotes.portfolio.api.PortfolioReferenceApi;
import com.starrainnotes.portfolio.api.PortfolioSearchSourceApi;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;
import com.starrainnotes.portfolio.exception.WorkNotFoundException;
import com.starrainnotes.portfolio.service.PortfolioWorkService;
import com.starrainnotes.portfolio.vo.WorkVO;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortfolioPublicApiImpl implements PortfolioPublicApi, PortfolioReferenceApi, PortfolioSearchSourceApi {
    private final PortfolioWorkService works;
    private final com.starrainnotes.portfolio.service.PortfolioContentService content;

    @Override
    public PageResult<PortfolioPublishedWork> publishedWorks(int page, int pageSize) {
        PageResult<WorkVO> pageResult = works.publicWorks(page, pageSize, null);
        return PageResult.<PortfolioPublishedWork>builder()
                .items(pageResult.getItems().stream().map(item -> published(works.publicWorkById(Long.valueOf(item.getId())))).toList())
                .page(pageResult.getPage()).pageSize(pageResult.getPageSize())
                .total(pageResult.getTotal()).build();
    }

    @Override
    public Optional<PortfolioPublishedWork> publishedWorkBySlug(String slug) {
        try {
            return Optional.of(published(works.publicWork(slug)));
        } catch (WorkNotFoundException exception) {
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(Long workId) {
        try {
            works.adminWork(workId);
            return true;
        } catch (WorkNotFoundException exception) {
            return false;
        }
    }

    @Override
    public Optional<PortfolioPublishedWork> publishedWork(Long workId) {
        try {
            return Optional.of(published(works.publicWorkById(workId)));
        } catch (WorkNotFoundException exception) {
            return Optional.empty();
        }
    }

    @Override
    public PageResult<PortfolioPublishedWork> sourcePage(int page, int pageSize) {
        return publishedWorks(page, pageSize);
    }

    private PortfolioPublishedWork published(WorkVO work) {
        return PortfolioPublishedWork.builder().id(Long.valueOf(work.getId()))
                .slug(work.getSlug()).workType(work.getWorkType()).title(work.getTitle())
                .summary(work.getSummary()).coverUrl(work.getCoverUrl())
                .bodyMarkdown(content.seoMarkdown(Long.valueOf(work.getId()), work.getBodyMarkdown()))
                .searchableText(work.getSearchableText()).seoTitle(work.getSeoTitle()).seoDescription(work.getSeoDescription())
                .publishedAt(work.getPublishedAt()).updatedAt(work.getUpdatedAt()).build();
    }
}
