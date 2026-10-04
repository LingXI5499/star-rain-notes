package com.starrainnotes.seo.source;

import java.util.List;
import java.util.Optional;

public interface SeoSourceProvider {
    boolean supports(String routePath);
    Optional<SeoSourceDocument> loadByRoute(String routePath);
    List<SeoSourceDocument> listPublished();
}
