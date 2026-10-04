package com.starrainnotes.portfolio.api;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;

public interface PortfolioPublicApi {
    PageResult<PortfolioPublishedWork> publishedWorks(int page, int pageSize);
}
