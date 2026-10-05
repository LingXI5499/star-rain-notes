package com.starrainnotes.portfolio.api;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;
import java.util.Optional;

public interface PortfolioPublicApi {
    PageResult<PortfolioPublishedWork> publishedWorks(int page, int pageSize);
    Optional<PortfolioPublishedWork> publishedWorkBySlug(String slug);
}
