package com.starrainnotes.search.service;

import com.starrainnotes.search.service.SearchRebuildService;

import com.starrainnotes.search.mapper.SearchDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchStartupRebuild {
    private static final Logger log = LoggerFactory.getLogger(SearchStartupRebuild.class);
    private final SearchDocumentMapper mapper;
    private final SearchRebuildService rebuild;

    @EventListener(ApplicationReadyEvent.class)
    public void seedEmptyIndex() {
        try {
            if (mapper.activeCount() == 0) {
                rebuild.rebuildAll();
                log.info("Search index initialized with {} public documents", mapper.activeCount());
            } else {
                // Existing installations already have other modules indexed; backfill English too.
                rebuild.rebuildType("ENGLISH");
            }
        } catch (RuntimeException exception) {
            log.error("Search startup rebuild failed; admin rebuild remains available", exception);
        }
    }
}
