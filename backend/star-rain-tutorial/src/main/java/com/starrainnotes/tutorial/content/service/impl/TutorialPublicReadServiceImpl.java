package com.starrainnotes.tutorial.content.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.analytics.api.AnalyticsRecordApi;
import com.starrainnotes.analytics.api.ContentViewEvent;
import com.starrainnotes.tutorial.content.service.TutorialPublicReadService;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TutorialPublicReadServiceImpl implements TutorialPublicReadService {
    private final TutorialPublicationService publications;
    private final ObjectProvider<AnalyticsRecordApi> analytics;

    @Override
    public JsonNode tutorial(String slug) {
        JsonNode detail = publications.publicTutorial(slug);
        record("TUTORIAL", detail, "/tutorials/:slug");
        return detail;
    }

    @Override
    public JsonNode chapter(String tutorialSlug, String chapterSlug) {
        JsonNode detail = publications.publicChapter(tutorialSlug, chapterSlug);
        record("CHAPTER", detail, "/tutorials/:slug/:chapterSlug");
        return detail;
    }

    private void record(String type, JsonNode detail, String route) {
        AnalyticsRecordApi recorder = analytics.getIfAvailable();
        if (recorder != null)
            recorder.recordContentView(new ContentViewEvent(type, detail.path("id").asLong(), route));
    }
}
