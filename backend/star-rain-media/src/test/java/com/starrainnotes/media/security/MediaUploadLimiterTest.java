package com.starrainnotes.media.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.starrainnotes.media.exception.MediaUploadRateLimitedException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class MediaUploadLimiterTest {

    @Test
    void rejectsTwentyFirstUploadForSameAccount() {
        MediaUploadLimiter limiter = new MediaUploadLimiter(
                Clock.fixed(Instant.parse("2026-10-05T00:00:00Z"), ZoneOffset.UTC));
        for (int attempt = 0; attempt < 20; attempt++) {
            limiter.check(1L);
        }
        assertThatThrownBy(() -> limiter.check(1L))
                .isInstanceOf(MediaUploadRateLimitedException.class)
                .hasMessageContaining("上传过于频繁");
        assertThatCode(() -> limiter.check(2L)).doesNotThrowAnyException();
    }

    @Test
    void newWindowAllowsUploadingAgain() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-05T00:00:00Z").toEpochMilli());
        MediaUploadLimiter limiter = new MediaUploadLimiter(clock);
        for (int attempt = 0; attempt < 20; attempt++) {
            limiter.check(1L);
        }
        clock.advance(59_999);
        assertThatThrownBy(() -> limiter.check(1L)).isInstanceOf(MediaUploadRateLimitedException.class);
        clock.advance(1);
        assertThatCode(() -> limiter.check(1L)).doesNotThrowAnyException();
    }

    @Test
    void concurrentRequestsCannotExceedLimit() {
        MediaUploadLimiter limiter = new MediaUploadLimiter(
                Clock.fixed(Instant.parse("2026-10-05T00:00:00Z"), ZoneOffset.UTC));
        AtomicInteger accepted = new AtomicInteger();
        IntStream.range(0, 100).parallel().forEach(attempt -> {
            try {
                limiter.check(1L);
                accepted.incrementAndGet();
            } catch (MediaUploadRateLimitedException ignored) {
                // 超限请求应被明确拒绝。
            }
        });
        org.assertj.core.api.Assertions.assertThat(accepted.get()).isEqualTo(20);
    }

    private static final class MutableClock extends Clock {
        private final AtomicLong currentMillis;

        private MutableClock(long initialMillis) {
            currentMillis = new AtomicLong(initialMillis);
        }

        private void advance(long millis) {
            currentMillis.addAndGet(millis);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis()); }
        @Override public long millis() { return currentMillis.get(); }
    }
}
