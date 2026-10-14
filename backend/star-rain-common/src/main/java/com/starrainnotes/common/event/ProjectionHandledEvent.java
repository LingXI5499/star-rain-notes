package com.starrainnotes.common.event;

/** In-process acknowledgement; the durable repair watermark remains in the business database. */
public record ProjectionHandledEvent(Object source, String projection, boolean successful) { }
