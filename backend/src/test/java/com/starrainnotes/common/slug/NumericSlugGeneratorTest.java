package com.starrainnotes.common.slug;

import com.starrainnotes.common.error.ApiException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NumericSlugGeneratorTest {

    @Test
    void generatesTwelveDigitsWithANonZeroFirstDigit() {
        String first = NumericSlugGenerator.generate(slug -> false);
        String second = NumericSlugGenerator.generate(slug -> false);

        assertThat(first).matches("^[1-9][0-9]{11}$");
        assertThat(second).matches("^[1-9][0-9]{11}$").isNotEqualTo(first);
    }

    @Test
    void retriesWhenTheGeneratedNumberIsAlreadyUsed() {
        AtomicInteger checks = new AtomicInteger();

        String slug = NumericSlugGenerator.generate(candidate -> checks.getAndIncrement() == 0);

        assertThat(slug).matches("^[1-9][0-9]{11}$");
        assertThat(checks).hasValue(2);
    }

    @Test
    void keepsExplicitLegacySlugsAndExistingSlugsOnUpdate() {
        assertThat(NumericSlugGenerator.forCreate("legacy-slug", slug -> false)).isEqualTo("legacy-slug");
        assertThat(NumericSlugGenerator.forUpdate("  ", "3-2")).isEqualTo("3-2");
    }

    @Test
    void failsClearlyAfterTheRetryLimit() {
        AtomicInteger checks = new AtomicInteger();

        assertThatThrownBy(() -> NumericSlugGenerator.generate(slug -> {
            checks.incrementAndGet();
            return true;
        }))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getCode())
                        .isEqualTo("SLUG_GENERATION_FAILED"));
        assertThat(checks).hasValue(NumericSlugGenerator.MAX_ATTEMPTS);
    }
}
