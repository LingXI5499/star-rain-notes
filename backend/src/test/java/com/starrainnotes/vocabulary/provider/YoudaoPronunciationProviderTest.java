package com.starrainnotes.vocabulary.provider;

import com.starrainnotes.vocabulary.config.VocabularyProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Youdao dictvoice URL construction and US/UK type mapping.
 * Pure unit test — no Spring context, no database, no network.
 */
class YoudaoPronunciationProviderTest {

    private final VocabularyProperties props = new VocabularyProperties(
            new VocabularyProperties.Providers("youdao"),
            new VocabularyProperties.Youdao("https://dict.youdao.com/dictvoice", "0", "1"),
            new VocabularyProperties.Pronunciation("US", "./data/pronunciation-cache"));
    private final YoudaoPronunciationProvider provider = new YoudaoPronunciationProvider(props);

    @Test
    void americanAccentUsesTypeZero() {
        assertThat(provider.pronunciationUrl("hello", "US"))
                .hasValue("https://dict.youdao.com/dictvoice?type=0&audio=hello");
    }

    @Test
    void britishAccentUsesTypeOne() {
        assertThat(provider.pronunciationUrl("hello", "UK"))
                .hasValue("https://dict.youdao.com/dictvoice?type=1&audio=hello");
    }

    @Test
    void accentIsCaseInsensitive() {
        assertThat(provider.pronunciationUrl("hello", "uk"))
                .hasValue("https://dict.youdao.com/dictvoice?type=1&audio=hello");
    }

    @Test
    void missingAccentFallsBackToDefault() {
        assertThat(provider.pronunciationUrl("hello", null))
                .hasValue("https://dict.youdao.com/dictvoice?type=0&audio=hello");
    }

    @Test
    void blankWordReturnsEmpty() {
        assertThat(provider.pronunciationUrl("", "US")).isEmpty();
        assertThat(provider.pronunciationUrl("   ", "US")).isEmpty();
    }
}
