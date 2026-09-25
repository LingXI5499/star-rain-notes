package com.starrainnotes.media.service;

import com.starrainnotes.common.error.ApiException;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes and removes media bytes inside a configured storage root. */
final class MediaFileStore {
    private MediaFileStore() {
    }

    static void assertWithinRoot(Path target, Path root) {
        if (!target.startsWith(root)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Storage error", "Resolved path escapes the media root.");
        }
    }

    static void write(Path target, byte[] bytes) {
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Storage unavailable", "The media file could not be stored.");
        }
    }

    static void deleteIfExists(Path target, Logger log, String message) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            log.warn(message, target, ex);
        }
    }

    static void deleteStoredFile(Path target, Logger log) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            log.warn("Failed to delete media file {} (row already removed)", target, ex);
        }
    }
}
