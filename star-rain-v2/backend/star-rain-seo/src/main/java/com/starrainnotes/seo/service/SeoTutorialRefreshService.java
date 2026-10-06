package com.starrainnotes.seo.service;

/* 教程发布状态变化后的 SEO 派生数据同步入口。 */
public interface SeoTutorialRefreshService {
    void publicationChanged(String slug, String action);
}
