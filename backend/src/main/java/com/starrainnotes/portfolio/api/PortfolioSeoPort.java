package com.starrainnotes.portfolio.api;

import java.time.LocalDateTime;
import java.util.List;

public interface PortfolioSeoPort {
    record Card(String title, String slug, String summary, LocalDateTime updatedAt) {
    }

    record Project(String title, String summary, String body, LocalDateTime publishedAt, LocalDateTime updatedAt,
                   Long coverMediaId, String repositoryUrl, String demoUrl) {
    }

    record State(String slug, boolean published) {
    }

    State visibility(long id);

    List<Card> published();

    Project publishedProject(String slug);
}
