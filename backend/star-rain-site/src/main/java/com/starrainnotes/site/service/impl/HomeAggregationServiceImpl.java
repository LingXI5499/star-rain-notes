/*
 * HomeAggregationServiceImpl：站点模块的业务实现文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：HomeSectionVO、SiteHomeVO、SitePublicApi、SitePublicConfigVO、HomeSectionEntity、HomeAggregationService、HomeSectionDisplayOptions。
 * 阅读入口：home（首页处理入口）；shutdown（shutdown处理入口）。
 */

package com.starrainnotes.site.service.impl;

import com.starrainnotes.site.vo.HomeSectionVO;
import com.starrainnotes.site.vo.SiteHomeVO;

import com.starrainnotes.site.api.SitePublicApi;
import com.starrainnotes.site.api.vo.SitePublicConfigVO;
import com.starrainnotes.site.entity.HomeSectionEntity;
import com.starrainnotes.site.service.HomeAggregationService;
import com.starrainnotes.site.provider.HomeSectionDisplayOptions;
import com.starrainnotes.site.provider.HomeSectionProvider;
import com.starrainnotes.site.service.HomeSectionService;
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

/**
 * 首页Aggregation业务服务实现，属于站点模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
public class HomeAggregationServiceImpl implements HomeAggregationService {
    /**
     * 当前类使用的日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(HomeAggregationService.class);
    /**
     * MS。
     * 初始值为 <code>1200</code>。
     */
    private static final long SECTION_BUDGET_MS = 1200;
    /**
     * 站点。
     * 协作类型：<code>SitePublicApi</code>。
     */
    private final SitePublicApi site;
    /**
     * sections。
     * 协作类型：<code>HomeSectionService</code>。
     */
    private final HomeSectionService sections;
    /**
     * 显示。
     */
    private final HomeSectionDisplayOptions display;
    /**
     * providers。
     */
    private final Map<String, HomeSectionProvider> providers = new HashMap<>();
    /**
     * workers。
     */
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(6, 6, 30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(24), task -> {
                Thread thread = new Thread(task, "site-home-section");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());

    /**
     * 构造HomeAggregationServiceImpl，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>this.providers.put</code>、<code>provider.sectionCode</code>。
     *
     * @param site 站点；类型为 <code>SitePublicApi</code>
     * @param sections sections；类型为 <code>HomeSectionService</code>
     * @param display 显示；类型为 <code>HomeSectionDisplayOptions</code>
     * @param providers providers；类型为 <code>List&lt;HomeSectionProvider&gt;</code>
     */
    public HomeAggregationServiceImpl(SitePublicApi site, HomeSectionService sections, HomeSectionDisplayOptions display,
                                  List<HomeSectionProvider> providers) {
        this.site = site;
        this.sections = sections;
        this.display = display;
        for (HomeSectionProvider provider : providers) this.providers.put(provider.sectionCode(), provider);
    }

    /**
     * 首页处理入口。
     * 主要协作调用：<code>site.config</code>、<code>sections.enabled</code>、<code>providers.get</code>、<code>workers.submit</code>、<code>display.layoutOf</code>。
     * @return 类型为 <code>SiteHomeVO</code> 的处理结果
     */
    @Override
    public SiteHomeVO home() {
        CachedHome current = cachedHome;
        long now = System.nanoTime();
        if (current != null && now < current.expiresAt()) return current.value();
        synchronized (cacheLock) {
            current = cachedHome;
            if (current != null && System.nanoTime() < current.expiresAt()) return current.value();
            long generation = cacheGeneration.get();
            SiteHomeVO result = loadHome();
            if (generation == cacheGeneration.get()) cachedHome = new CachedHome(result, System.nanoTime() + TimeUnit.SECONDS.toNanos(15));
            return result;
        }
    }

    private final Object cacheLock = new Object();
    private final java.util.concurrent.atomic.AtomicLong cacheGeneration = new java.util.concurrent.atomic.AtomicLong();
    private volatile CachedHome cachedHome;
    private record CachedHome(SiteHomeVO value, long expiresAt) { }

    @org.springframework.transaction.event.TransactionalEventListener(fallbackExecution = true)
    public void invalidateHome(Object event) {
        if (event instanceof org.springframework.context.PayloadApplicationEvent<?> payload) event = payload.getPayload();
        String name = event.getClass().getName();
        if (!name.startsWith("com.starrainnotes.") || !name.contains(".event.")) return;
        cacheGeneration.incrementAndGet();
        cachedHome = null;
    }

    private SiteHomeVO loadHome() {
        SitePublicConfigVO config = site.config();
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
        List<HomeSectionVO> result = new ArrayList<>();
        for (HomeSectionEntity section : enabled) {
            String code = section.getSectionCode();
            if ("HERO".equals(code)) {
                result.add(new HomeSectionVO(code, section.getDisplayName(), config, "READY", display.layoutOf(section)));
                continue;
            }
            Future<Object> future = futures.get(code);
            if (future == null) {
                result.add(degraded(section));
                continue;
            }
            try {
                long remaining = Math.max(0, deadlines.get(code) - System.nanoTime());
                result.add(new HomeSectionVO(code, section.getDisplayName(),
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
        return new SiteHomeVO(config, result);
    }

    /**
     * degraded处理入口。
     * 主要协作调用：<code>display.layoutOf</code>。
     *
     * @param section 区块；类型为 <code>HomeSectionEntity</code>
     * @return 类型为 <code>HomeSectionVO</code> 的处理结果
     */
    private HomeSectionVO degraded(HomeSectionEntity section) {
        return new HomeSectionVO(section.getSectionCode(), section.getDisplayName(), List.of(), "DEGRADED", display.layoutOf(section));
    }

    /**
     * shutdown处理入口。
     * 主要协作调用：<code>workers.shutdownNow</code>。
     */
    @PreDestroy
    public void shutdown() { workers.shutdownNow(); }
}
