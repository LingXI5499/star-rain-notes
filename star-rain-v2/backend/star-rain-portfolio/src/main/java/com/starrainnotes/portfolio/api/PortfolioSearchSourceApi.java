package com.starrainnotes.portfolio.api;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;

public interface PortfolioSearchSourceApi {
    PageResult<PortfolioPublishedWork> sourcePage(int page, int pageSize);
}
