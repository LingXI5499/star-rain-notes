package com.starrainnotes.english.vocabulary.domain;

import java.util.Arrays;

/** Canonical six-section CET4 taxonomy shared by public and admin flows. */
public enum VocabularyLayer {
    GENERAL(1, "基础通用词层"),
    DAILY_LIFE(2, "核心生活场景层"),
    STUDY_AND_WORK(3, "学习与工作主干层"),
    SOCIETY_AND_WORLD(4, "社会与世界主题层"),
    READING_AND_LOGIC(5, "抽象阅读与逻辑层"),
    LANGUAGE_SYSTEM(6, "语言系统层");

    private final int order;
    private final String label;

    VocabularyLayer(int order, String label) {
        this.order = order;
        this.label = label;
    }

    public int order() {
        return order;
    }

    public String label() {
        return label;
    }

    public static VocabularyLayer fromOrder(int order) {
        return Arrays.stream(values())
                .filter(layer -> layer.order == order)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown vocabulary layer order: " + order));
    }
}
