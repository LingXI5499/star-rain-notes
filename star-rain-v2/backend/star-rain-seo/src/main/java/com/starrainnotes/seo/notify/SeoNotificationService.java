package com.starrainnotes.seo.notify;

import com.starrainnotes.seo.canonical.CanonicalService;
import com.starrainnotes.seo.mapper.SeoNotificationMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeoNotificationService {
    private static final Logger log = LoggerFactory.getLogger(SeoNotificationService.class);
    private final SeoNotificationMapper mapper;
    private final List<SearchEngineNotifier> notifiers;
    private final CanonicalService canonical;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueue(String routePath, String changeType) {
        String url = canonical.canonical(routePath);
        if (!("UPSERT".equals(changeType) || "DELETE".equals(changeType))) {
            throw new IllegalArgumentException("Invalid SEO change type");
        }
        for (SearchEngineNotifier notifier : notifiers) {
            if (!notifier.enabled()) continue;
            SeoNotificationLog row = new SeoNotificationLog();
            row.setProviderCode(notifier.providerCode());
            row.setRoutePath(routePath);
            row.setCanonicalUrl(url);
            row.setChangeType(changeType);
            mapper.enqueue(row);
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void processPending() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (SeoNotificationLog row : mapper.due(now, 50)) {
            SearchEngineNotifier notifier = notifiers.stream()
                .filter(item -> item.enabled() && item.providerCode().equals(row.getProviderCode()))
                .findFirst().orElse(null);
            if (notifier == null || mapper.claim(row.getId(), now) == 0) continue;
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
