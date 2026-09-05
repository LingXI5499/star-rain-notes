package com.starrainnotes.vocabulary.provider;

import com.starrainnotes.vocabulary.config.VocabularyProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Youdao dictionary pronunciation ({@code dict.youdao.com/dictvoice}).
 *
 * <p>Enabled only when {@code app.vocabulary.providers.pronunciation=youdao};
 * otherwise {@link DisabledVocabularyProviders} remains the active provider.
 * This bean is {@code @Primary} for {@link PronunciationProvider} so it wins
 * over the always-present disabled fallback. The actual audio is fetched
 * server-side by the pronunciation service; this provider only builds the
 * upstream URL.</p>
 */
@Component
@Primary
@ConditionalOnProperty(name = "app.vocabulary.providers.pronunciation", havingValue = "youdao")
public class YoudaoPronunciationProvider implements PronunciationProvider {

    private final VocabularyProperties props;

    public YoudaoPronunciationProvider(VocabularyProperties props) {
        this.props = props;
    }

    @Override
    public Optional<String> pronunciationUrl(String word, String accent) {
        String clean = word == null ? "" : word.trim();
        if (clean.isEmpty()) {
            return Optional.empty();
        }
        boolean uk = accent != null && accent.equalsIgnoreCase("UK");
        String type = uk ? props.youdao().ukType() : props.youdao().usType();
        String base = props.youdao().dictVoiceBaseUrl();
        String encoded = URLEncoder.encode(clean, StandardCharsets.UTF_8);
        return Optional.of(base + (base.contains("?") ? "&" : "?") + "type=" + type + "&audio=" + encoded);
    }
}
