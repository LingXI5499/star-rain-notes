package com.starrainnotes.seo.service;

import com.starrainnotes.seo.api.SeoRefreshApi;
import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.dto.SeoSourceDocument;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeoRebuildService {
    private final List<SeoSourceProvider> providers;
    private final SeoSnapshotService snapshots;
    private final SeoRefreshApi refresh;
    private final SeoPageMapper mapper;

    public long rebuild(String routePath) {
        if (routePath == null || routePath.isBlank()) rebuildAll();
        else rebuildRoute(routePath);
        return mapper.activeCount();
    }

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

    public void rebuildRoute(String routePath) { refresh.refreshRoute(routePath); }
}
