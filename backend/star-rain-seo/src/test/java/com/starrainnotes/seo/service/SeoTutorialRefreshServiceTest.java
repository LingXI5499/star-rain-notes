/*
 * SeoTutorialRefreshServiceTest：公开页面与 SEO模块的测试文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：SeoPageMapper、SeoTutorialRefreshServiceImpl、SeoSnapshotService、SeoNotificationService、TutorialSeoSourceProvider。
 * 阅读入口：chapterWithdrawalRefreshesPublicTreeAndRemovesOnlyMissingSeoPath（章节WithdrawalRefreshes公开Tree及RemovesOnly缺失SEO路径处理入口）；withdrawalNotificationsRunOnlyAfterSnapshotCommit（withdrawalNotificationsRunOnly之后快照Commit处理入口）。
 */

package com.starrainnotes.seo.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.service.impl.SeoTutorialRefreshServiceImpl;
import com.starrainnotes.seo.service.SeoSnapshotService;
import com.starrainnotes.seo.service.SeoNotificationService;
import com.starrainnotes.seo.provider.impl.TutorialSeoSourceProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * SEO教程刷新业务服务测试，属于公开页面与 SEO模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class SeoTutorialRefreshServiceTest {
    /**
     * 章节WithdrawalRefreshes公开Tree及RemovesOnly缺失SEO路径处理入口。
     * 关键处理步骤调用 <code>mapper.activePathsByPrefix</code>、<code>List.of</code>、<code>com.starrainnotes.seo.dto.SeoSourceDocument.builder</code>、<code>tutorials.publishedTree</code>。
     */
    @Test
    void chapterWithdrawalRefreshesPublicTreeAndRemovesOnlyMissingSeoPath() {
        var tutorials=mock(TutorialSeoSourceProvider.class);var snapshots=mock(SeoSnapshotService.class);
        var mapper=mock(SeoPageMapper.class);var notifications=mock(SeoNotificationService.class);
        String removed="/tutorials/java/one",retained="/tutorials/java/two";
        when(mapper.activePathsByPrefix("/tutorials/java/"))
            .thenReturn(List.of(removed,retained),List.of(removed,retained),List.of(retained));
        var root=com.starrainnotes.seo.dto.SeoSourceDocument.builder().routePath("/tutorials/java").build();
        var chapter=com.starrainnotes.seo.dto.SeoSourceDocument.builder().routePath(retained).build();
        when(tutorials.publishedTree("java")).thenReturn(List.of(root,chapter));
        new SeoTutorialRefreshServiceImpl(tutorials,snapshots,mapper,notifications).publicationChanged("java","PUBLISHED");
        verify(snapshots).upsert(root);verify(snapshots).upsert(chapter);
        verify(mapper).remove(removed);verify(mapper,never()).remove(retained);
        verify(notifications).enqueue(removed,"DELETE");verify(notifications).enqueue(retained,"UPSERT");
    }

    /**
     * withdrawalNotificationsRunOnly之后快照Commit处理入口。
     * 关键处理步骤调用 <code>mapper.activePathsByPrefix</code>、<code>List.of</code>、<code>TransactionSynchronizationManager.initSynchronization</code>、<code>service.publicationChanged</code>。
     */
    @Test
    void withdrawalNotificationsRunOnlyAfterSnapshotCommit() {
        TutorialSeoSourceProvider tutorials = mock(TutorialSeoSourceProvider.class);
        SeoSnapshotService snapshots = mock(SeoSnapshotService.class);
        SeoPageMapper mapper = mock(SeoPageMapper.class);
        SeoNotificationService notifications = mock(SeoNotificationService.class);
        String chapter = "/tutorials/java/chapter-1";
        when(mapper.activePathsByPrefix("/tutorials/java/")).thenReturn(List.of(chapter));
        SeoTutorialRefreshService service =
            new SeoTutorialRefreshServiceImpl(tutorials, snapshots, mapper, notifications);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.publicationChanged("java", "WITHDRAWN");
            verify(mapper).remove("/tutorials/java");
            verify(mapper).remove(chapter);
            verify(notifications, never()).enqueue("/tutorials/java", "DELETE");
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(notifications).enqueue("/tutorials/java", "DELETE");
            verify(notifications).enqueue(chapter, "DELETE");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
