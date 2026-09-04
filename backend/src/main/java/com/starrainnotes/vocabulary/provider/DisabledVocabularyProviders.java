package com.starrainnotes.vocabulary.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DisabledVocabularyProviders implements DictionaryProvider, PronunciationProvider, TextToSpeechProvider {
    @Override
    public DictionaryPreview preview(String word) {
        return new DictionaryPreview("DISABLED", false, word, null, null, List.of(), List.of(),
                "外部词典默认关闭。未取得允许缓存和公开展示的授权前，仅使用本站已核验内容与系统朗读。");
    }

    @Override
    public Optional<String> pronunciationUrl(String word, String accent) {
        return Optional.empty();
    }

    @Override
    public Optional<byte[]> synthesize(String text, String voice) {
        return Optional.empty();
    }
}
