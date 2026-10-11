/*
 * MediaUploadLimiterTest：媒体模块的测试文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：MediaUploadRateLimitedException。
 * 阅读入口：getZone（读取Zone）；withZone（包含Zone处理入口）；instant（instant处理入口）；millis（millis处理入口）。
 */

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

/**
 * 媒体上传Limiter测试，属于媒体模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class MediaUploadLimiterTest {

    /**
     * 验证拒绝Twenty首次上传面向Same账号。
     * 关键处理步骤调用 <code>Clock.fixed</code>、<code>Instant.parse</code>、<code>limiter.check</code>。
     */
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

    /**
     * new时间窗口AllowsUploadingAgain处理入口。
     * 关键处理步骤调用 <code>Instant.parse</code>、<code>limiter.check</code>、<code>clock.advance</code>。
     */
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

    /**
     * concurrentRequestsCannotExceed上限处理入口。
     * 关键处理步骤调用 <code>Clock.fixed</code>、<code>Instant.parse</code>、<code>IntStream.range</code>、<code>limiter.check</code>。
     */
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

    /**
     * MutableClock，属于媒体模块。
     * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
     */
    private static final class MutableClock extends Clock {
        /**
         * 当前Millis。
         */
        private final AtomicLong currentMillis;

        /**
         * 构造MutableClock，接收本类使用的依赖或初始化参数。
         *
         * @param initialMillis initialMillis；类型为 <code>long</code>
         */
        private MutableClock(long initialMillis) {
            currentMillis = new AtomicLong(initialMillis);
        }

        /**
         * advance处理入口。
         * 主要协作调用：<code>currentMillis.addAndGet</code>。
         *
         * @param millis millis；类型为 <code>long</code>
         */
        private void advance(long millis) {
            currentMillis.addAndGet(millis);
        }

        /**
         * 读取Zone。
         * @return 类型为 <code>ZoneId</code> 的处理结果
         */
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        /**
         * 包含Zone处理入口。
         *
         * @param zone zone；类型为 <code>ZoneId</code>
         * @return 类型为 <code>Clock</code> 的处理结果
         */
        @Override public Clock withZone(ZoneId zone) { return this; }
        /**
         * instant处理入口。
         * 关键处理步骤调用 <code>Instant.ofEpochMilli</code>、<code>millis</code>。
         * @return 类型为 <code>Instant</code> 的处理结果
         */
        @Override public Instant instant() { return Instant.ofEpochMilli(millis()); }
        /**
         * millis处理入口。
         * 主要协作调用：<code>currentMillis.get</code>。
         * @return 类型为 <code>long</code> 的处理结果
         */
        @Override public long millis() { return currentMillis.get(); }
    }
}
