package com.starrainnotes.seo.service;

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
public class SeoTutorialRefreshService {
    private final TutorialSeoSourceProvider tutorials;
    private final SeoSnapshotService snapshots;
    private final SeoPageMapper mapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refresh(String slug) {
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void remove(String slug) {
        String chapterPrefix = prefix(slug);
        mapper.remove("/tutorials/" + slug);
        for (String path : mapper.activePathsByPrefix(chapterPrefix)) mapper.remove(path);
    }

    private String prefix(String slug) {
        if (slug == null || !slug.matches("[a-zA-Z0-9_-]{1,120}")) throw new IllegalArgumentException("Invalid tutorial slug");
        return "/tutorials/" + slug + "/";
    }
}
