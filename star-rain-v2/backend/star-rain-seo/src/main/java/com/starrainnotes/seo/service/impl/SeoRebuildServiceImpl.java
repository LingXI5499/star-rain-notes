package com.starrainnotes.seo.service.impl;

import com.starrainnotes.seo.api.SeoRefreshApi;
import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.dto.SeoSourceDocument;
import com.starrainnotes.seo.service.SeoRebuildService;
import com.starrainnotes.seo.service.SeoSnapshotService;
import com.starrainnotes.seo.service.SeoSourceProvider;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeoRebuildServiceImpl implements SeoRebuildService {
    private final List<SeoSourceProvider> providers;
    private final SeoSnapshotService snapshots;
    private final SeoRefreshApi refresh;
    private final SeoPageMapper mapper;

    @Override
    public long rebuild(String routePath) {
        if (routePath == null || routePath.isBlank()) rebuildAll();
        else rebuildRoute(routePath);
        return mapper.activeCount();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rebuildAll() {
        Set<String> seen = new HashSet<>();
        for (SeoSourceProvider provider : providers) {
            for (SeoSourceDocument source : provider.listPublished()) {
                snapshots.upsert(source);
                seen.add(source.getRoutePath());
            }
        }
        for (String path : mapper.activePaths()) {
            if (!seen.contains(path)) mapper.remove(path);
        }
    }

    @Override
    public void rebuildRoute(String routePath) { refresh.refreshRoute(routePath); }
}
