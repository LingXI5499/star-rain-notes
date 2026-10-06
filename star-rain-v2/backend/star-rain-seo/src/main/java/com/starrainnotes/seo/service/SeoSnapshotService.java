package com.starrainnotes.seo.service;

import com.starrainnotes.seo.dto.SeoSourceDocument;

public interface SeoSnapshotService {
    void upsert(SeoSourceDocument source);
}
