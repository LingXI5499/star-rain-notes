package com.starrainnotes.seo;

import static org.junit.jupiter.api.Assertions.*;
import com.starrainnotes.seo.config.SeoProperties;
import com.starrainnotes.seo.renderer.SeoInteractiveAssetRenderer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SeoAssetCacheTest {
    @TempDir Path directory;
    private static final String HTML = "<html><head></head><body><h1>Semantic content</h1></body></html>";
    @Test void reusesImmutableEntryRefreshesChangedBuildAndAvoidsDuplicates() throws Exception {
        Path index = directory.resolve("index.html");
        Files.writeString(index, "<script type=\"module\" src=\"/assets/old.js\"></script><link rel=\"stylesheet\" href=\"/assets/old.css\">");
        SeoProperties properties = new SeoProperties(); properties.setFrontendIndexPath(index.toString());
        var renderer = new SeoInteractiveAssetRenderer(properties);
        String first = renderer.include(HTML);
        assertTrue(first.contains("/assets/old.js"));
        assertEquals(first, renderer.include(first));
        // A same-version file edit proves repeated requests reuse the parsed cache.
        FileTime modified = Files.getLastModifiedTime(index);
        Files.writeString(index, Files.readString(index).replace("old", "new"));
        Files.setLastModifiedTime(index, modified);
        assertEquals(first, renderer.include(HTML));
        Files.setLastModifiedTime(index, FileTime.fromMillis(modified.toMillis() + 10_000));
        try (var pool = Executors.newFixedThreadPool(8)) {
            var tasks = java.util.stream.IntStream.range(0, 32)
                    .mapToObj(i -> pool.submit(() -> renderer.include(HTML))).toList();
            for (var task : tasks) assertTrue(task.get().contains("/assets/new.js"));
        }
        Files.delete(index);
        assertEquals(HTML, renderer.include(HTML));
        assertEquals(HTML, renderer.include(HTML));
    }
}
