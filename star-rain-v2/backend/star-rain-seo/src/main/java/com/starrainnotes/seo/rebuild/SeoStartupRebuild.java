package com.starrainnotes.seo.rebuild;

import com.starrainnotes.seo.mapper.SeoPageMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeoStartupRebuild {
    private static final Logger log = LoggerFactory.getLogger(SeoStartupRebuild.class);
    private final SeoPageMapper mapper;
    private final SeoRebuildService rebuild;

    @EventListener(ApplicationReadyEvent.class)
    public void seedEmptySnapshots() {
        try {
            if (mapper.activeCount() == 0) {
                rebuild.rebuildAll();
                log.info("SEO snapshots initialized with {} public pages", mapper.activeCount());
            }
        } catch (RuntimeException exception) {
            log.error("SEO startup rebuild failed; admin rebuild remains available", exception);
        }
    }
}
