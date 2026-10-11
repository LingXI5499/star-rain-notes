/*
 * SeoSnapshotServiceImpl：公开页面与 SEO模块的业务实现文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：SeoPageSnapshot、SeoPageApi、SeoRefreshApi、SeoPageMapper、SeoPageModel、SeoSourceDocument、SeoSourceProvider。
 * 阅读入口：getByRoute（读取依据路由）；getMetaByRoute（读取Meta依据路由）；refreshRoute（刷新路由）；removeRoute（移除路由）。
 */

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

/**
 * SEO快照业务服务实现，属于公开页面与 SEO模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class SeoSnapshotServiceImpl implements SeoSnapshotService, SeoPageApi, SeoRefreshApi {
    /**
     * providers。
     */
    private final List<SeoSourceProvider> providers;
    /**
     * 数据映射。
     * 协作类型：<code>SeoPageMapper</code>。
     */
    private final SeoPageMapper mapper;
    /**
     * 规范。
     */
    private final CanonicalUrlResolver canonical;
    /**
     * 渲染器。
     */
    private final SeoHtmlRenderer renderer;

    /**
     * 读取依据路由。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>canonical.canonical</code>、<code>mapper.active</code>。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<SeoPageSnapshot> getByRoute(String routePath) {
        canonical.canonical(routePath);
        return Optional.ofNullable(mapper.active(routePath));
    }

    /**
     * 读取Meta依据路由。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>canonical.canonical</code>、<code>mapper.activeMeta</code>。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<SeoPageSnapshot> getMetaByRoute(String routePath) {
        canonical.canonical(routePath);
        return Optional.ofNullable(mapper.activeMeta(routePath));
    }

    /**
     * 刷新路由。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>canonical.canonical</code>、<code>providers.stream</code>、<code>mapper.remove</code>。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     */
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

    /**
     * 移除路由。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>canonical.canonical</code>、<code>mapper.remove</code>。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeRoute(String routePath) {
        canonical.canonical(routePath);
        mapper.remove(routePath);
    }

    /**
     * 新增或更新SEO快照。
     * 主要协作调用：<code>canonical.canonical</code>、<code>renderer.render</code>、<code>mapper.upsert</code>。
     *
     * @param source 来源；类型为 <code>SeoSourceDocument</code>
     */
    @Override
    public void upsert(SeoSourceDocument source) {
        String route = source.getRoutePath();
        String url = canonical.canonical(route);
        String title = SeoMetaFormatter.title(source);
        String description = SeoMetaFormatter.description(source);
        String robots = "index,follow";
        String html = renderer.render(SeoPageModel.builder().canonicalUrl(url).title(title)
            .description(description).robotsDirective(robots).bodyMarkdown(source.getBodyMarkdown())
            .coverUrl(source.getCoverUrl()).contentType(source.getContentType()).updatedAt(source.getUpdatedAt()).build());
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
