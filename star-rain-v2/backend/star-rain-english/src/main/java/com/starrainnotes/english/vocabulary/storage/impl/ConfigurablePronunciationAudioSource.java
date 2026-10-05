package com.starrainnotes.english.vocabulary.storage.impl;

import com.starrainnotes.english.vocabulary.properties.VocabularyPronunciationProperties;
import com.starrainnotes.english.vocabulary.storage.PronunciationAudioSource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/*
 * 配置驱动的发音来源。
 *
 * 当前只实现有道 dictvoice：/dictvoice?type=<0 美式|1 英式>&audio=<word>。
 * 之所以不做成「有多个 @Component 实现 + @ConditionalOnProperty 二选一」，是因为
 * 条件装配在遇到未识别的 provider 值时会一个都不生效，导致注入失败而不是「自动降级为关闭」。
 * 这里把开关收敛在一个实现里：不认识的取值一律按 disabled 处理，行为可预期。
 */
@Component
@RequiredArgsConstructor
public class ConfigurablePronunciationAudioSource implements PronunciationAudioSource {

    private static final String YOUDAO = "youdao";

    private final VocabularyPronunciationProperties properties;

    @Override
    public Optional<String> audioUrl(String word, String accent) {
        if (!enabled()) {
            return Optional.empty();
        }
        String clean = word == null ? "" : word.trim();
        if (clean.isEmpty()) {
            return Optional.empty();
        }
        boolean uk = "UK".equals(accent == null ? "" : accent.toUpperCase(Locale.ROOT));
        String type = uk ? properties.getUkType() : properties.getUsType();
        String base = properties.getDictVoiceBaseUrl();
        String encoded = URLEncoder.encode(clean, StandardCharsets.UTF_8);
        String separator = base.contains("?") ? "&" : "?";
        return Optional.of(base + separator + "type=" + type + "&audio=" + encoded);
    }

    @Override
    public boolean enabled() {
        return properties.isEnabled() && YOUDAO.equalsIgnoreCase(providerOrDefault());
    }

    @Override
    public String providerName() {
        return enabled() ? YOUDAO : "disabled";
    }

    private String providerOrDefault() {
        String provider = properties.getProvider();
        return provider == null || provider.isBlank() ? "disabled" : provider.trim();
    }
}
