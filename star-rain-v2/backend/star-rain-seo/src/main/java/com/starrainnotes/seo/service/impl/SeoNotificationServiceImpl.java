package com.starrainnotes.seo.service.impl;

import com.starrainnotes.seo.dto.NotificationResult;
import com.starrainnotes.seo.dto.SeoChange;
import com.starrainnotes.seo.entity.SeoNotificationLog;
import com.starrainnotes.seo.notifier.IndexNowNotifier;
import com.starrainnotes.seo.mapper.SeoNotificationMapper;
import com.starrainnotes.seo.service.SeoNotificationService;
import com.starrainnotes.seo.config.CanonicalUrlResolver;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeoNotificationServiceImpl implements SeoNotificationService {
    private static final Logger log = LoggerFactory.getLogger(SeoNotificationServiceImpl.class);
    private final SeoNotificationMapper mapper;
    private final IndexNowNotifier notifier;
    private final CanonicalUrlResolver canonical;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueue(String routePath, String changeType) {
        String url = canonical.canonical(routePath);
        if (!("UPSERT".equals(changeType) || "DELETE".equals(changeType))) {
            throw new IllegalArgumentException("Invalid SEO change type");
        }
        if (!notifier.enabled()) return;
        SeoNotificationLog row = new SeoNotificationLog();
        row.setProviderCode(notifier.providerCode());
        row.setRoutePath(routePath);
        row.setCanonicalUrl(url);
        row.setChangeType(changeType);
        mapper.enqueue(row);
    }

    @Override
    public void processPending() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (SeoNotificationLog row : mapper.due(now, 50)) {
            if (!notifier.enabled() || !notifier.providerCode().equals(row.getProviderCode())
                    || mapper.claim(row.getId(), now) == 0) continue;
            NotificationResult result;
            try {
                result = notifier.notify(SeoChange.builder().routePath(row.getRoutePath())
                    .canonicalUrl(row.getCanonicalUrl()).changeType(row.getChangeType()).build());
            } catch (RuntimeException exception) {
                result = NotificationResult.builder().errorCode("PROVIDER_ERROR").build();
            }
            if (result.isSuccess()) mapper.success(row.getId(), result.getHttpStatus());
            else {
                int minutes = Math.min(60, 1 << Math.min(6, row.getAttemptCount() + 1));
                mapper.failure(row.getId(), result.getHttpStatus(),
                    result.getErrorCode() == null ? "NOTIFICATION_FAILED" : result.getErrorCode(),
                    now.plusMinutes(minutes));
                log.warn("SEO notification failed for provider {} and route {}; retry scheduled",
                    row.getProviderCode(), row.getRoutePath());
            }
        }
    }
}
