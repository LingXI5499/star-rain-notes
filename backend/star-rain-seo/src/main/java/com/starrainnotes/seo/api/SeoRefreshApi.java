package com.starrainnotes.seo.api;

public interface SeoRefreshApi {
    void refreshRoute(String routePath);
    void removeRoute(String routePath);
}
