package com.starrainnotes.seo;

import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.assertj.core.api.Assertions.assertThat;

class BoundedSeoCacheTest {
    @Test void expiresAndEvictsLeastRecentlyUsedWithoutCachingFailures() {
        AtomicLong time = new AtomicLong();
        BoundedSeoCache<String> cache = new BoundedSeoCache<>(2, 100, time::get);
        cache.get("a", () -> "a");
        cache.get("b", () -> "b");
        assertThat(cache.get("a", () -> "wrong")).isEqualTo("a");
        cache.get("c", () -> "c");
        assertThat(cache.get("b", () -> "new b")).isEqualTo("new b");
        time.set(101);
        assertThat(cache.get("b", () -> "expired")).isEqualTo("expired");
        assertThat(cache.get("missing", () -> null)).isNull();
        assertThat(cache.get("missing", () -> "published")).isEqualTo("published");
    }

    @Test void invalidationDuringLoadCannotResurrectWithdrawnContent() throws Exception {
        BoundedSeoCache<String> cache = new BoundedSeoCache<>(10, 1000, System::currentTimeMillis);
        CountDownLatch entered = new CountDownLatch(1), resume = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> request = executor.submit(() -> cache.get("post", () -> {
                if (calls.incrementAndGet() == 1) {
                    entered.countDown();
                    try { if (!resume.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("timeout"); }
                    catch (InterruptedException e) { throw new RuntimeException(e); }
                    return "old published content";
                }
                return null;
            }));
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                cache.clear();
            } finally { resume.countDown(); }
            assertThat(request.get(5, TimeUnit.SECONDS)).isNull();
            assertThat(calls.get()).isEqualTo(2);
        }
    }

    @Test void concurrentRequestsLoadOnlyOnce() throws Exception {
        BoundedSeoCache<String> cache = new BoundedSeoCache<>(10, 1000, System::currentTimeMillis);
        AtomicInteger calls = new AtomicInteger();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = java.util.stream.IntStream.range(0, 20).mapToObj(i -> executor.submit(() ->
                    cache.get("same", () -> { calls.incrementAndGet(); return "value"; }))).toList();
            for (var future : futures) assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo("value");
            assertThat(calls.get()).isEqualTo(1);
        }
    }
}
