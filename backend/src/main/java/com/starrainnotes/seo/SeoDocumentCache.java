package com.starrainnotes.seo;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.function.Supplier;

@Component
public class SeoDocumentCache {
    private static final long TTL_MILLIS = Duration.ofMinutes(5).toMillis();
    private final BoundedSeoCache<SeoPage> pages = new BoundedSeoCache<>(256, TTL_MILLIS, System::currentTimeMillis);

    public SeoPage page(String path, Supplier<SeoPage> loader) {
        return pages.get(path, loader);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void contentChanged(SeoContentChangedEvent ignored) {
        pages.clear();
    }

}
