package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ModuleBoundaryTest {
    private static final Pattern IMPORT = Pattern.compile("^import com\\.starrainnotes\\.([a-z]+)\\.(.+);$");
    private static final Set<String> MODULES = Set.of("account", "analytics", "blog", "english", "media",
        "message", "portfolio", "profile", "review", "search", "seo", "site", "tutorial");

    @Test
    void businessModulesOnlyImportOtherModulesPublicContracts() throws IOException {
        Path backend = backendRoot();
        List<String> violations = new ArrayList<>();
        for (String source : MODULES) {
            Path javaRoot = backend.resolve("star-rain-" + source).resolve("src/main/java");
            try (var files = Files.walk(javaRoot)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    for (String line : Files.readAllLines(file)) {
                        Matcher match = IMPORT.matcher(line);
                        if (!match.matches()) continue;
                        String target = match.group(1);
                        String name = match.group(2);
                        if (!MODULES.contains(target) || source.equals(target)) continue;
                        if (name.startsWith("api.") || name.startsWith("event.")
                            || name.contains(".api.") || name.contains(".event.")) continue;
                        violations.add(backend.relativize(file) + " imports " + line);
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }

    private Path backendRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("star-rain-blog/src/main/java"))) return current;
            if (Files.isDirectory(current.resolve("backend/star-rain-blog/src/main/java"))) return current.resolve("backend");
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate the V2 backend reactor");
    }
}
