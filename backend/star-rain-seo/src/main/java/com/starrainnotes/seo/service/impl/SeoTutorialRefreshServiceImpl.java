/*
 * SeoTutorialRefreshServiceImpl：公开页面与 SEO模块的业务实现文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：SeoPageMapper、SeoSourceDocument、SeoTutorialRefreshService、SeoSnapshotService、SeoNotificationService、TutorialSeoSourceProvider。
 * 阅读入口：publicationChanged（发布Changed处理入口）；afterCommit（之后Commit处理入口）。
 */

package com.starrainnotes.seo.service.impl;

import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.dto.SeoSourceDocument;
import com.starrainnotes.seo.service.SeoTutorialRefreshService;
import com.starrainnotes.seo.service.SeoSnapshotService;
import com.starrainnotes.seo.service.SeoNotificationService;
import com.starrainnotes.seo.provider.impl.TutorialSeoSourceProvider;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * SEO教程刷新业务服务实现，属于公开页面与 SEO模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class SeoTutorialRefreshServiceImpl implements SeoTutorialRefreshService {
    /**
     * tutorials。
     */
    private final TutorialSeoSourceProvider tutorials;
    /**
     * snapshots。
     * 协作类型：<code>SeoSnapshotService</code>。
     */
    private final SeoSnapshotService snapshots;
    /**
     * 数据映射。
     * 协作类型：<code>SeoPageMapper</code>。
     */
    private final SeoPageMapper mapper;
    /**
     * notifications。
     * 协作类型：<code>SeoNotificationService</code>。
     */
    private final SeoNotificationService notifications;

    // 发布事务提交后同步派生快照；通知等快照事务提交后再入队。
    /**
     * 发布Changed处理入口。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.activePathsByPrefix</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param action 动作；类型为 <code>String</code>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publicationChanged(String slug, String action) {
        if (!"WITHDRAWN".equals(action) && !"PUBLISHED".equals(action) && !"RESTORED".equals(action)) return;
        String route = "/tutorials/" + slug;
        String chapterPrefix = prefix(slug);
        Set<String> before = new HashSet<>(mapper.activePathsByPrefix(chapterPrefix));
        if ("WITHDRAWN".equals(action)) {
            removeTree(slug);
            notifyAfterCommit(route, "DELETE", List.of(), before);
        } else if ("PUBLISHED".equals(action) || "RESTORED".equals(action)) {
            refreshTree(slug);
            List<String> after = mapper.activePathsByPrefix(chapterPrefix);
            before.removeAll(after);
            notifyAfterCommit(route, "UPSERT", after, before);
        }
    }

    /**
     * 通知之后Commit。
     * 主要协作调用：<code>notifications.enqueue</code>。
     *
     * @param root root；类型为 <code>String</code>
     * @param rootAction root动作；类型为 <code>String</code>
     * @param chapters 章节列表；类型为 <code>List&lt;String&gt;</code>
     * @param removed 已移除；类型为 <code>Set&lt;String&gt;</code>
     */
    private void notifyAfterCommit(String root, String rootAction, List<String> chapters, Set<String> removed) {
        List<String> upserts = List.copyOf(chapters);
        List<String> deletes = List.copyOf(removed);
        Runnable send = () -> {
            notifications.enqueue(root, rootAction);
            upserts.forEach(path -> notifications.enqueue(path, "UPSERT"));
            deletes.forEach(path -> notifications.enqueue(path, "DELETE"));
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
    /**
     * 当前调用使用的匿名协作实现，属于公开页面与 SEO模块。
     * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
     */
    {
                /**
                 * 之后Commit处理入口。
                 * 关键处理步骤调用 <code>send.run</code>。
                 */
                @Override public void afterCommit() { send.run(); }
            });
        } else send.run();
    }

    /**
     * 刷新Tree。
     * 主要协作调用：<code>tutorials.publishedTree</code>、<code>snapshots.upsert</code>、<code>mapper.activePathsByPrefix</code>、<code>mapper.remove</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     */
    private void refreshTree(String slug) {
        List<SeoSourceDocument> documents = tutorials.publishedTree(slug);
        Set<String> seen = new HashSet<>();
        for (SeoSourceDocument document : documents) {
            snapshots.upsert(document);
            seen.add(document.getRoutePath());
        }
        for (String path : mapper.activePathsByPrefix(prefix(slug))) {
            if (!seen.contains(path)) mapper.remove(path);
        }
    }

    /**
     * 移除Tree。
     * 主要协作调用：<code>mapper.remove</code>、<code>mapper.activePathsByPrefix</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     */
    private void removeTree(String slug) {
        String chapterPrefix = prefix(slug);
        mapper.remove("/tutorials/" + slug);
        for (String path : mapper.activePathsByPrefix(chapterPrefix)) mapper.remove(path);
    }

    /**
     * prefix处理入口。
     * 关键处理步骤调用 <code>slug.matches</code>。
     * 实现中的显式失败类型包括 <code>IllegalArgumentException</code>；具体触发条件见对应分支。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String prefix(String slug) {
        if (slug == null || !slug.matches("[a-zA-Z0-9_-]{1,120}")) throw new IllegalArgumentException("Invalid tutorial slug");
        return "/tutorials/" + slug + "/";
    }
}
