package com.starrainnotes.seo;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.seo.config.CanonicalUrlResolver;
import com.starrainnotes.seo.mapper.SeoNotificationMapper;
import com.starrainnotes.seo.dto.NotificationResult;
import com.starrainnotes.seo.notifier.IndexNowNotifier;
import com.starrainnotes.seo.dto.SeoChange;
import com.starrainnotes.seo.entity.SeoNotificationLog;
import com.starrainnotes.seo.service.impl.SeoNotificationServiceImpl;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SeoNotificationServiceTest {
    @Test
    void failedProviderIsRetriedWithoutRecordingASecret() {
        SeoNotificationMapper mapper = mock(SeoNotificationMapper.class);
        IndexNowNotifier notifier = mock(IndexNowNotifier.class);
        CanonicalUrlResolver canonical = mock(CanonicalUrlResolver.class);
        when(notifier.enabled()).thenReturn(true);
        when(notifier.providerCode()).thenReturn("TEST");
        SeoNotificationLog row = new SeoNotificationLog();
        row.setId(7L);
        row.setProviderCode("TEST");
        row.setRoutePath("/blog/posts/test");
        row.setCanonicalUrl("https://example.org/blog/posts/test");
        row.setChangeType("UPSERT");
        row.setAttemptCount(0);
        when(mapper.due(any(), eq(50))).thenReturn(List.of(row));
        when(mapper.claim(eq(7L), any())).thenReturn(1);
        when(notifier.notify(any(SeoChange.class))).thenReturn(
            NotificationResult.builder().httpStatus(503).errorCode("HTTP_503").build());

        new SeoNotificationServiceImpl(mapper, notifier, canonical).processPending();

        ArgumentCaptor<LocalDateTime> next = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).failure(eq(7L), eq(503), eq("HTTP_503"), next.capture());
        assertTrue(next.getValue().isAfter(LocalDateTime.now(ZoneOffset.UTC)));
    }
}
