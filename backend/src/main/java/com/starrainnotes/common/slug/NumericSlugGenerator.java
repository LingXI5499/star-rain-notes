package com.starrainnotes.common.slug;

import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;

import java.security.SecureRandom;
import java.util.function.Predicate;

/** Generates opaque, fixed-width numeric slugs for newly created content. */
public final class NumericSlugGenerator {

    static final int MAX_ATTEMPTS = 20;
    private static final long MIN_VALUE = 100_000_000_000L;
    private static final long MAX_VALUE_EXCLUSIVE = 1_000_000_000_000L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private NumericSlugGenerator() {
    }

    public static String forCreate(String requestedSlug, Predicate<String> exists) {
        String requested = clean(requestedSlug);
        return requested != null ? requested : generate(exists);
    }

    public static String forUpdate(String requestedSlug, String existingSlug) {
        String requested = clean(requestedSlug);
        return requested != null ? requested : existingSlug;
    }

    public static String generate(Predicate<String> exists) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = Long.toString(RANDOM.nextLong(MIN_VALUE, MAX_VALUE_EXCLUSIVE));
            if (!exists.test(candidate)) {
                return candidate;
            }
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SLUG_GENERATION_FAILED",
                "Unable to generate content number",
                "A unique content number could not be generated. Please retry.");
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
