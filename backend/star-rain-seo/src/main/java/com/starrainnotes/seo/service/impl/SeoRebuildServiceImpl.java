/*
 * SeoRebuildServiceImpl：公开页面与 SEO模块的业务实现文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：SeoRefreshApi、SeoPageMapper、SeoSourceDocument、SeoRebuildService、SeoSnapshotService、SeoSourceProvider。
 * 阅读入口：rebuild（重建SEORebuild）；rebuildAll（重建全部）；rebuildRoute（重建路由）。
 */

package com.starrainnotes.seo.service.impl;

import com.starrainnotes.seo.api.SeoRefreshApi;
import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.dto.SeoSourceDocument;
import com.starrainnotes.seo.service.SeoRebuildService;
import com.starrainnotes.seo.service.SeoSnapshotService;
import com.starrainnotes.seo.provider.SeoSourceProvider;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * SEORebuild业务服务实现，属于公开页面与 SEO模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class SeoRebuildServiceImpl implements SeoRebuildService {
    /**
     * providers。
     */
    private final List<SeoSourceProvider> providers;
    /**
     * snapshots。
     * 协作类型：<code>SeoSnapshotService</code>。
     */
    private final SeoSnapshotService snapshots;
    /**
     * 刷新。
     * 协作类型：<code>SeoRefreshApi</code>。
     */
    private final SeoRefreshApi refresh;
    /**
     * 数据映射。
     * 协作类型：<code>SeoPageMapper</code>。
     */
    private final SeoPageMapper mapper;

    /**
     * 重建SEORebuild。
     * 主要协作调用：<code>mapper.activeCount</code>。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
    @Override
    public long rebuild(String routePath) {
        if (routePath == null || routePath.isBlank()) rebuildAll();
        else rebuildRoute(routePath);
        return mapper.activeCount();
    }

    /**
     * 重建全部。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>snapshots.upsert</code>、<code>mapper.activePaths</code>、<code>mapper.remove</code>。
     */
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

    /**
     * 重建路由。
     * 主要协作调用：<code>refresh.refreshRoute</code>。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     */
    @Override
    public void rebuildRoute(String routePath) { refresh.refreshRoute(routePath); }
}
