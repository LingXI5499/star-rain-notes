package com.starrainnotes.vocabulary.dto;

public record VocabularyAudioView(
        Long id,
        String accent,
        Long mediaAssetId,
        String publicUrl,
        String provider,
        String sourceUrl,
        String licenseNote,
        boolean primary) {
}
