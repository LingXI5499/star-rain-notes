package com.starrainnotes.seo;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class SeoDocumentCache {
    private static final long TTL_MILLIS = Duration.ofMinutes(5).toMillis();
    private final ConcurrentHashMap<String, Entry> pages = new ConcurrentHashMap<>();

    public SeoPage page(String path, Supplier<SeoPage> loader) {
        long now = System.currentTimeMillis();
        Entry cached = pages.get(path);
        if (cached != null && cached.expiresAt() > now) return cached.page();
        SeoPage loaded = loader.get();
        if (loaded != null) pages.put(path, new Entry(loaded, now + TTL_MILLIS));
        else pages.remove(path);
        return loaded;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void contentChanged(SeoContentChangedEvent ignored) {
        pages.clear();
    }

    private record Entry(SeoPage page, long expiresAt) {}
}
