package com.starrainnotes.vocabulary.provider;

import java.util.List;

public record DictionaryPreview(
        String provider,
        boolean enabled,
        String word,
        String phoneticUk,
        String phoneticUs,
        List<String> explanations,
        List<String> examples,
        String notice) {
}
