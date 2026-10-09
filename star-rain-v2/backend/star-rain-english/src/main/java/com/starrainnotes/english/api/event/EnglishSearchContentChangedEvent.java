package com.starrainnotes.english.api.event;

import lombok.Value;

/* A null contentId requests reconciliation of a type after a parent content change. */
@Value
public class EnglishSearchContentChangedEvent {
    String contentType;
    Long contentId;
}
