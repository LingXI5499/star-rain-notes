package com.starrainnotes.media.upload;

import com.starrainnotes.media.exception.MediaUploadRateLimitedException;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** 单实例按账号限制上传次数，避免连续文件写入耗尽存储和处理资源。 */
@Component
public class MediaUploadLimiter {

    private static final int MAX_UPLOADS_PER_MINUTE = 20;
    private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();
    private final ConcurrentHashMap<Long, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public MediaUploadLimiter() {
        this(Clock.systemUTC());
    }

    MediaUploadLimiter(Clock clock) {
        this.clock = clock;
    }

    public void check(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException("Authenticated account ID is required");
        }
        long now = clock.millis();
        if (windows.size() > 4096) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_MILLIS);
        }
        Window window = windows.compute(accountId, (ignored, old) ->
                old == null || now - old.startedAt >= WINDOW_MILLIS
                        ? new Window(now, 1) : new Window(old.startedAt, old.count + 1));
        if (window.count > MAX_UPLOADS_PER_MINUTE) {
            throw new MediaUploadRateLimitedException();
        }
    }

    private record Window(long startedAt, int count) { }
}
