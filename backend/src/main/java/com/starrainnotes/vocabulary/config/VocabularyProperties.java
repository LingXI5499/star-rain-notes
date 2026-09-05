package com.starrainnotes.vocabulary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

/**
 * Vocabulary feature configuration (relaxed binding from {@code app.vocabulary.*}).
 *
 * <p>{@code providers.pronunciation} selects the word-pronunciation source:
 * {@code youdao} enables the Youdao dictvoice provider, otherwise the disabled
 * fallback (browser system speech) remains active. No secret/API key required.</p>
 */
@ConfigurationProperties("app.vocabulary")
public record VocabularyProperties(
        Providers providers,
        Youdao youdao,
        Pronunciation pronunciation) {

    public record Providers(String pronunciation) {
        public Providers {
            pronunciation = blank(pronunciation) ? "disabled" : pronunciation.trim();
        }
    }

    public record Youdao(String dictVoiceBaseUrl, String usType, String ukType) {
        public Youdao {
            dictVoiceBaseUrl = blank(dictVoiceBaseUrl) ? "https://dict.youdao.com/dictvoice" : dictVoiceBaseUrl.trim();
            usType = blank(usType) ? "0" : usType.trim();
            ukType = blank(ukType) ? "1" : ukType.trim();
        }
    }

    public record Pronunciation(String defaultAccent, String cacheDir) {
        public Pronunciation {
            defaultAccent = blank(defaultAccent) ? "US" : defaultAccent.trim().toUpperCase(Locale.ROOT);
            cacheDir = blank(cacheDir) ? "./data/pronunciation-cache" : cacheDir.trim();
        }
    }

    public VocabularyProperties {
        providers = providers == null ? new Providers(null) : providers;
        youdao = youdao == null ? new Youdao(null, null, null) : youdao;
        pronunciation = pronunciation == null ? new Pronunciation(null, null) : pronunciation;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
