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

@Service
@RequiredArgsConstructor
public class SeoTutorialRefreshServiceImpl implements SeoTutorialRefreshService {
    private final TutorialSeoSourceProvider tutorials;
    private final SeoSnapshotService snapshots;
    private final SeoPageMapper mapper;
    private final SeoNotificationService notifications;

    // 发布事务提交后同步派生快照；通知等快照事务提交后再入队。
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

    private void notifyAfterCommit(String root, String rootAction, List<String> chapters, Set<String> removed) {
        List<String> upserts = List.copyOf(chapters);
        List<String> deletes = List.copyOf(removed);
        Runnable send = () -> {
            notifications.enqueue(root, rootAction);
            upserts.forEach(path -> notifications.enqueue(path, "UPSERT"));
            deletes.forEach(path -> notifications.enqueue(path, "DELETE"));
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { send.run(); }
            });
        } else send.run();
    }

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

    private void removeTree(String slug) {
        String chapterPrefix = prefix(slug);
        mapper.remove("/tutorials/" + slug);
        for (String path : mapper.activePathsByPrefix(chapterPrefix)) mapper.remove(path);
    }

    private String prefix(String slug) {
        if (slug == null || !slug.matches("[a-zA-Z0-9_-]{1,120}")) throw new IllegalArgumentException("Invalid tutorial slug");
        return "/tutorials/" + slug + "/";
    }
}
