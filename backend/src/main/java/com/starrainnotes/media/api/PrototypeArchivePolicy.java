package com.starrainnotes.media.api;

/** Shared ZIP size and magic-byte rule for prototype archives. */
public final class PrototypeArchivePolicy {
    public static final long MAX_BYTES = 25L * 1024 * 1024;

    private PrototypeArchivePolicy() {
    }

    public static boolean withinLimit(byte[] bytes) {
        return bytes != null && bytes.length <= MAX_BYTES;
    }

    public static boolean hasZipMagic(byte[] bytes) {
        return bytes != null && bytes.length >= 2 && bytes[0] == 'P' && bytes[1] == 'K';
    }
}
