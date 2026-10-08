package com.starrainnotes.seo.service.impl;

import com.starrainnotes.seo.api.dto.SeoPageSnapshot;

import com.starrainnotes.seo.api.SeoPageApi;
import com.starrainnotes.seo.api.SeoRefreshApi;
import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.dto.SeoPageModel;
import com.starrainnotes.seo.dto.SeoSourceDocument;
import com.starrainnotes.seo.provider.SeoSourceProvider;
import com.starrainnotes.seo.renderer.SeoHtmlRenderer;
import com.starrainnotes.seo.service.SeoSnapshotService;
import com.starrainnotes.seo.config.CanonicalUrlResolver;
import com.starrainnotes.seo.utils.SeoMetaFormatter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeoSnapshotServiceImpl implements SeoSnapshotService, SeoPageApi, SeoRefreshApi {
    private final List<SeoSourceProvider> providers;
    private final SeoPageMapper mapper;
    private final CanonicalUrlResolver canonical;
    private final SeoHtmlRenderer renderer;

    @Override
    @Transactional(readOnly = true)
    public Optional<SeoPageSnapshot> getByRoute(String routePath) {
        canonical.canonical(routePath);
        return Optional.ofNullable(mapper.active(routePath));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SeoPageSnapshot> getMetaByRoute(String routePath) {
        canonical.canonical(routePath);
        return Optional.ofNullable(mapper.activeMeta(routePath));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshRoute(String routePath) {
        canonical.canonical(routePath);
        Optional<SeoSourceDocument> source = providers.stream()
            .filter(provider -> provider.supports(routePath)).findFirst()
            .flatMap(provider -> provider.loadByRoute(routePath));
        if (source.isPresent()) upsert(source.get());
        else mapper.remove(routePath);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeRoute(String routePath) {
        canonical.canonical(routePath);
        mapper.remove(routePath);
    }

    @Override
    public void upsert(SeoSourceDocument source) {
        String route = source.getRoutePath();
        String url = canonical.canonical(route);
        String title = SeoMetaFormatter.title(source);
        String description = SeoMetaFormatter.description(source);
        String robots = "index,follow";
        String html = renderer.render(SeoPageModel.builder().canonicalUrl(url).title(title)
            .description(description).robotsDirective(robots).bodyMarkdown(source.getBodyMarkdown())
            .coverUrl(source.getCoverUrl()).build());
        SeoPageSnapshot snapshot = new SeoPageSnapshot();
        snapshot.setRoutePath(route);
        snapshot.setContentType(source.getContentType());
        snapshot.setContentId(source.getContentId());
        snapshot.setCanonicalUrl(url);
        snapshot.setTitle(title);
        snapshot.setDescription(description);
        snapshot.setRobotsDirective(robots);
        snapshot.setHtmlSnapshot(html);
        snapshot.setSourceVersionRef(source.getUpdatedAt() == null ? null : source.getUpdatedAt().toString());
        mapper.upsert(snapshot);
    }
}
