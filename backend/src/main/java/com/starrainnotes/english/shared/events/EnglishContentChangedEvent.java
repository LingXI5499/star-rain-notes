package com.starrainnotes.english.shared.events;

public record EnglishContentChangedEvent(
        EnglishContentKind contentType, long contentId, String slug,
        EnglishContentChangeType changeType) { }
