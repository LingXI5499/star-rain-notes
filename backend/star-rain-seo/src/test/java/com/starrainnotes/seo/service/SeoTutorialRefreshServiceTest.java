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

class SeoTutorialRefreshServiceTest {
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
