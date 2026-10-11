package com.starrainnotes.blog.service.impl;

import com.starrainnotes.analytics.api.AnalyticsRecordApi;
import com.starrainnotes.analytics.api.ContentViewEvent;
import com.starrainnotes.blog.service.BlogPublicReadService;
import com.starrainnotes.blog.service.BlogPublicService;
import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BlogPublicReadServiceImpl implements BlogPublicReadService {
    private final BlogPublicService posts;
    private final ObjectProvider<AnalyticsRecordApi> analytics;

    @Override
    public BlogPostPublicDetailVO read(String slug) {
        BlogPostPublicDetailVO detail = posts.postBySlug(slug);
        AnalyticsRecordApi recorder = analytics.getIfAvailable();
        if (recorder != null)
            recorder.recordContentView(new ContentViewEvent("BLOG", detail.getId(), "/blog/posts/:slug"));
        return detail;
    }
}
