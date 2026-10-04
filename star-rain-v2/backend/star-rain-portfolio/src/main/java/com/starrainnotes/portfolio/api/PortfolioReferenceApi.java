package com.starrainnotes.portfolio.api;

import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;
import java.util.Optional;

public interface PortfolioReferenceApi {
    boolean exists(Long workId);
    Optional<PortfolioPublishedWork> publishedWork(Long workId);
}
