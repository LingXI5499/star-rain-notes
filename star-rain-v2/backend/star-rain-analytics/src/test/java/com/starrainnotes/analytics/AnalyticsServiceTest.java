package com.starrainnotes.analytics;

import com.starrainnotes.analytics.service.AnalyticsAggregateService;
import com.starrainnotes.analytics.api.ContentViewEvent;
import com.starrainnotes.analytics.service.ReferrerClassifier;
import com.starrainnotes.analytics.entity.AnalyticsEventEntity;
import com.starrainnotes.analytics.exception.AnalyticsDateRangeInvalidException;
import com.starrainnotes.analytics.exception.AnalyticsRouteInvalidException;
import com.starrainnotes.analytics.mapper.AnalyticsMapper;
import com.starrainnotes.analytics.service.impl.AnalyticsServiceImpl;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AnalyticsServiceTest {
    private final AnalyticsMapper mapper = mock(AnalyticsMapper.class);
    private final AnalyticsServiceImpl service = new AnalyticsServiceImpl(mapper, new ReferrerClassifier());

    @Test
    void pageViewOnlyAcceptsKnownPublicRoutes() {
        assertThrows(AnalyticsRouteInvalidException.class, () -> service.recordPageView("/useradmin/dashboard", null));
        verifyNoInteractions(mapper);
        service.recordPageView("/blog", "https://www.google.com/search?q=secret");
        org.mockito.ArgumentCaptor<AnalyticsEventEntity> capture = org.mockito.ArgumentCaptor.forClass(AnalyticsEventEntity.class);
        verify(mapper).insert(capture.capture());
        assertEquals("SEARCH", capture.getValue().getReferrerCategory());
        assertEquals("www.google.com", capture.getValue().getReferrerHost());
        assertFalse(capture.getValue().getReferrerHost().contains("secret"));
    }

    @Test
    void configuredSiteHostIsInternal() {
        ReferrerClassifier classifier = new ReferrerClassifier();
        ReflectionTestUtils.setField(classifier, "frontendOrigin", "https://notes.example.com");
        assertEquals("INTERNAL", classifier.classify("https://notes.example.com/post?private=1").getCategory());
        assertEquals("notes.example.com", classifier.classify("https://notes.example.com/post?private=1").getHost());
    }

    @Test
    void analyticsInsertFailureDoesNotFailPublicContent() {
        doThrow(new IllegalStateException("database unavailable")).when(mapper).insert(any());
        assertDoesNotThrow(() -> service.recordContentView(new ContentViewEvent("BLOG", 42L, "/blog/posts/:slug")));
    }

    @Test
    void invalidContentAndDateRangeAreRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> service.recordContentView(new ContentViewEvent("BLOG", 0L, "/blog/posts/:slug")));
        assertThrows(AnalyticsDateRangeInvalidException.class,
            () -> service.trend(LocalDate.now().minusDays(400), LocalDate.now()));
    }

    @Test
    void aggregateReplacesRatherThanIncrementsDay() {
        AnalyticsAggregateService aggregate = new AnalyticsAggregateService(mapper);
        LocalDate yesterday = LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1);
        aggregate.aggregateDay(yesterday);
        aggregate.aggregateDay(yesterday);
        verify(mapper, times(2)).deleteSiteDay(yesterday);
        verify(mapper, times(2)).deleteContentDay(yesterday);
        verify(mapper, times(2)).deleteReferrerDay(yesterday);
        verify(mapper, times(2)).insertSiteDay(yesterday);
        verify(mapper, times(2)).insertContentDay(yesterday);
        verify(mapper, times(2)).insertReferrerDay(yesterday);
    }
}
