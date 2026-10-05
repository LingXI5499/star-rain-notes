package com.starrainnotes.seo.service;

import com.starrainnotes.seo.dto.SeoSourceDocument;

import java.util.List;
import java.util.Optional;

public interface SeoSourceProvider {
    boolean supports(String routePath);
    Optional<SeoSourceDocument> loadByRoute(String routePath);
    List<SeoSourceDocument> listPublished();
}
