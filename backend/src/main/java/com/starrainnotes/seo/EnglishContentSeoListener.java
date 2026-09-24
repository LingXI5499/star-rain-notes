package com.starrainnotes.seo;

import com.starrainnotes.english.shared.events.EnglishContentChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EnglishContentSeoListener {
    private final ApplicationEventPublisher events;
    private final SeoProperties properties;

    public EnglishContentSeoListener(ApplicationEventPublisher events, SeoProperties properties) {
        this.events = events;
        this.properties = properties;
    }

    @EventListener
    public void contentChanged(EnglishContentChangedEvent change) {
        String pathPrefix = switch (change.contentType()) {
            case READING -> "/english/reading/";
            case LISTENING -> "/english/listening/";
            case GRAMMAR_LESSON -> "/english/grammar/";
            case WRITING_PROMPT -> "/english/writing/practice/";
            case WRITING_RESOURCE -> "/english/writing/resources/";
            case PRONUNCIATION_RULE -> "/english/listening/pronunciation/";
            case BUNDLE -> "/english/bundles/";
        };
        events.publishEvent(new SeoContentChangedEvent(properties.siteOrigin()
                + pathPrefix + change.slug()));
    }
}
