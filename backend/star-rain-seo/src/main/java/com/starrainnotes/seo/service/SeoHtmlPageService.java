package com.starrainnotes.seo.service;

import java.util.Optional;

// SEO 静态 HTML 页面读取入口。
public interface SeoHtmlPageService {

    Optional<String> html(String path);
}
