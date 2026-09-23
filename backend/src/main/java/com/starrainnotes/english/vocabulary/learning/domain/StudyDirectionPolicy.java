package com.starrainnotes.english.vocabulary.learning.domain;

public final class StudyDirectionPolicy {
    public String direction(String setting, long wordId, long siteEpochDay) {
        if (!"MIXED".equals(setting)) return setting;
        return ((wordId + siteEpochDay) & 1L) == 0 ? "EN_TO_ZH" : "ZH_TO_EN";
    }
}
