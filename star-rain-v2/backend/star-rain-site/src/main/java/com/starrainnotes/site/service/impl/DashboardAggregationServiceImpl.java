package com.starrainnotes.site.service.impl;

import com.starrainnotes.site.vo.SiteDashboardVO;
import com.starrainnotes.site.service.DashboardAggregationService;

import com.starrainnotes.common.dashboard.api.DashboardSource;
import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import com.starrainnotes.common.dashboard.dto.DashboardRecentItem;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class DashboardAggregationServiceImpl implements DashboardAggregationService {
    private static final Logger log = LoggerFactory.getLogger(DashboardAggregationServiceImpl.class);
    private static final long SOURCE_BUDGET_MS = 1500;
    private final List<DashboardSource> sources;
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(7, 7, 30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(28), task -> {
                Thread thread = new Thread(task, "site-dashboard-source");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());

    public DashboardAggregationServiceImpl(List<DashboardSource> sources) {
        Set<String> codes = new HashSet<>();
        for (DashboardSource source : sources) {
            String code = source.moduleCode();
            if (code == null || code.isBlank() || !codes.add(code)) {
                throw new IllegalArgumentException("Invalid or duplicate dashboard module code: " + code);
            }
        }
        this.sources = List.copyOf(sources);
    }

    @Override
    public SiteDashboardVO dashboard() {
        Map<String, Future<DashboardModuleData>> futures = new LinkedHashMap<>();
        Map<String, Long> deadlines = new HashMap<>();
        List<String> degraded = new ArrayList<>();
        for (DashboardSource source : sources) {
            try {
                futures.put(source.moduleCode(), workers.submit(source::load));
                deadlines.put(source.moduleCode(), System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(SOURCE_BUDGET_MS));
            } catch (RuntimeException exception) {
                log.warn("Site dashboard source {} could not be scheduled", source.moduleCode(), exception);
                degraded.add(source.moduleCode());
            }
        }
        Map<String, DashboardModuleData> modules = new LinkedHashMap<>();
        List<DashboardRecentItem> recent = new ArrayList<>();
        for (Map.Entry<String, Future<DashboardModuleData>> entry : futures.entrySet()) {
            String code = entry.getKey();
            Future<DashboardModuleData> future = entry.getValue();
            try {
                long remaining = Math.max(0, deadlines.get(code) - System.nanoTime());
                DashboardModuleData data = future.get(remaining, TimeUnit.NANOSECONDS);
                if (data == null || data.getMetrics() == null || data.getRecentContent() == null) {
                    throw new IllegalStateException("Dashboard source returned incomplete data");
                }
                modules.put(code, data);
                recent.addAll(data.getRecentContent());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                future.cancel(true);
                degraded.add(code);
                futures.forEach((remainingCode, pending) -> {
                    if (!modules.containsKey(remainingCode) && !degraded.contains(remainingCode)) {
                        pending.cancel(true);
                        degraded.add(remainingCode);
                    }
                });
                break;
            } catch (ExecutionException | TimeoutException | IllegalStateException exception) {
                future.cancel(true);
                log.warn("Site dashboard source {} degraded", code, exception);
                degraded.add(code);
            }
        }
        recent.sort(Comparator.comparing(DashboardRecentItem::getUpdatedAt,
                Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
        return new SiteDashboardVO(modules, recent.stream().limit(8).toList(), degraded);
    }

    @PreDestroy
    public void shutdown() { workers.shutdownNow(); }
}
