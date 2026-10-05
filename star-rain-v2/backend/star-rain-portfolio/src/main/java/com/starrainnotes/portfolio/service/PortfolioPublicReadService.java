package com.starrainnotes.portfolio.service;

import com.starrainnotes.analytics.api.AnalyticsRecordApi;
import com.starrainnotes.analytics.api.ContentViewEvent;
import com.starrainnotes.portfolio.vo.WorkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortfolioPublicReadService {
    private final PortfolioWorkService works;
    private final ObjectProvider<AnalyticsRecordApi> analytics;

    public WorkVO read(String slug) {
        WorkVO detail = works.publicWork(slug);
        AnalyticsRecordApi recorder = analytics.getIfAvailable();
        if (recorder != null)
            recorder.recordContentView(new ContentViewEvent(
                    "PORTFOLIO", Long.valueOf(detail.getId()), "/portfolio/:slug"));
        return detail;
    }
}
