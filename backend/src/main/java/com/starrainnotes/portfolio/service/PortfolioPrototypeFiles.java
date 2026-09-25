package com.starrainnotes.portfolio.service;

import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

/** Copies, deletes, and bounds paths inside an extracted prototype tree. */
final class PortfolioPrototypeFiles {
    private PortfolioPrototypeFiles() {
    }

    static Path safeRelative(String name) {
        if (name == null || name.contains("\\") || name.startsWith("/") || name.contains(":")) {
            throw invalid("Invalid archive path.");
        }
        Path path = Path.of(name).normalize();
        if (path.isAbsolute() || path.startsWith("..") || path.toString().contains("..")) {
            throw invalid("Invalid archive path.");
        }
        return path;
    }

    static void copyTree(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path from : paths.toList()) {
                Path to = target.resolve(source.relativize(from));
                if (Files.isDirectory(from)) {
                    Files.createDirectories(to);
                } else {
                    Files.createDirectories(to.getParent());
                    Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    static void deleteTree(Path root) {
        try {
            if (!Files.exists(root)) {
                return;
            }
            try (var paths = Files.walk(root)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // best effort
                    }
                });
            }
        } catch (IOException ignored) {
            // best effort
        }
    }

    private static ApiException invalid(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PROTOTYPE_ARCHIVE",
                "Invalid prototype archive", detail);
    }
}
