package com.starrainnotes.seo.service;

// SEO 快照重建入口：全量重建、按路由重建。
public interface SeoRebuildService {

    long rebuild(String routePath);

    void rebuildAll();

    void rebuildRoute(String routePath);
}
