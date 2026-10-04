package com.starrainnotes.seo.rebuild;

import com.starrainnotes.seo.api.SeoRefreshApi;
import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.snapshot.SeoSnapshotService;
import com.starrainnotes.seo.source.SeoSourceDocument;
import com.starrainnotes.seo.source.SeoSourceProvider;
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
