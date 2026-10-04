package com.starrainnotes.site.home;

import com.starrainnotes.site.api.SitePublicApi;
import com.starrainnotes.site.config.SitePublicConfig;
import com.starrainnotes.site.home.provider.HomeSectionProvider;
import com.starrainnotes.site.home.provider.HomeSectionLimit;
import com.starrainnotes.site.section.HomeSectionEntity;
import com.starrainnotes.site.section.HomeSectionService;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HomeAggregationService {
    private static final Logger log = LoggerFactory.getLogger(HomeAggregationService.class);
    private static final long SECTION_BUDGET_MS = 1200;
    private final SitePublicApi site;
    private final HomeSectionService sections;
    private final HomeSectionLimit display;
    private final Map<String, HomeSectionProvider> providers = new HashMap<>();
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(6, 6, 30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(24), task -> {
                Thread thread = new Thread(task, "site-home-section");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());

    public HomeAggregationService(SitePublicApi site, HomeSectionService sections, HomeSectionLimit display,
                                  List<HomeSectionProvider> providers) {
        this.site = site;
        this.sections = sections;
        this.display = display;
        for (HomeSectionProvider provider : providers) this.providers.put(provider.sectionCode(), provider);
    }

    public SiteHomeView home() {
        SitePublicConfig config = site.config();
        List<HomeSectionEntity> enabled = sections.enabled();
        Map<String, Future<Object>> futures = new HashMap<>();
        Map<String, Long> deadlines = new HashMap<>();
        for (HomeSectionEntity section : enabled) {
            HomeSectionProvider provider = providers.get(section.getSectionCode());
            if (provider == null || "HERO".equals(section.getSectionCode())) continue;
            try {
                futures.put(section.getSectionCode(), workers.submit(() -> provider.load(section, config)));
                deadlines.put(section.getSectionCode(), System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(SECTION_BUDGET_MS));
            } catch (RuntimeException exception) {
                log.warn("Site home section {} could not be scheduled", section.getSectionCode(), exception);
            }
        }
        List<HomeSectionData> result = new ArrayList<>();
        for (HomeSectionEntity section : enabled) {
            String code = section.getSectionCode();
            if ("HERO".equals(code)) {
                result.add(new HomeSectionData(code, section.getDisplayName(), config, "READY", display.layoutOf(section)));
                continue;
            }
            Future<Object> future = futures.get(code);
            if (future == null) {
                result.add(degraded(section));
                continue;
            }
            try {
                long remaining = Math.max(0, deadlines.get(code) - System.nanoTime());
                result.add(new HomeSectionData(code, section.getDisplayName(),
                        future.get(remaining, TimeUnit.NANOSECONDS), "READY", display.layoutOf(section)));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                future.cancel(true);
                result.add(degraded(section));
            } catch (ExecutionException | TimeoutException exception) {
                future.cancel(true);
                log.warn("Site home section {} degraded", code, exception);
                result.add(degraded(section));
            }
        }
        return new SiteHomeView(config, result);
    }

    private HomeSectionData degraded(HomeSectionEntity section) {
        return new HomeSectionData(section.getSectionCode(), section.getDisplayName(), List.of(), "DEGRADED", display.layoutOf(section));
    }

    @PreDestroy
    public void shutdown() { workers.shutdownNow(); }
}
