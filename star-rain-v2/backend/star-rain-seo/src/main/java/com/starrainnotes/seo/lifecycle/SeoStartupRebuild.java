package com.starrainnotes.seo.lifecycle;

import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.service.SeoRebuildService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/*
 * 启动时给空库补一份 SEO 快照。
 *
 * 它监听 ApplicationReadyEvent，属于应用生命周期，不是业务领域事件。
 *
 * 名字保留 Startup 前缀，是为了让「只在启动时跑一次」这件事在类名上就能看出来。
 */
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
