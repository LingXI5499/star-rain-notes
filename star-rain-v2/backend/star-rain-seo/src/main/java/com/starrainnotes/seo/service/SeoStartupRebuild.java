package com.starrainnotes.seo.service;

import com.starrainnotes.seo.mapper.SeoPageMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/*
 * 启动时给空库补一份 SEO 快照。
 *
 * 为什么放在 service 而不是 event：
 * 它监听的是 ApplicationReadyEvent（应用生命周期事件），不是业务领域事件；
 * 本模块的 event 包放的是领域事件消费者（SeoContentEventConsumer），混在一起会让
 * 「event 包 = 领域事件消费者」这条语义失效。而且 search 模块的同类类
 * （SearchStartupRebuild）本来就在 service 里，两边放一处才不会分散在不同深度。
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
