package com.starrainnotes.vocabulary.dto;

import java.util.List;

/**
 * One word layer (e.g. "基础通用词层A") with its themes, ordered.
 */
public record VocabularyLayerView(
        String layer,
        int layerOrder,
        List<VocabularyThemeView> themes) {
}
