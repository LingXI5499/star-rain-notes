package com.starrainnotes.portfolio.api;

import java.time.LocalDateTime;
import java.util.List;

public interface PortfolioPublishedPort {
    record Project(long id, String title, String slug, LocalDateTime updatedAt) {
    }

    record Featured(long id, String title, String slug, String summary, Long coverMediaId, String projectStatus) {
    }

    List<Project> latestPublished(int limit);

    List<Featured> featured(int limit);
}
