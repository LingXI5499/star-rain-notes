package com.starrainnotes.media.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Serves uploaded media at {@code /uploads/**} directly from the local
 * storage directory for dev / smoke / non-Nginx environments (TASK-012 P0 fix:
 * without a handler, media public URLs were 401/404 outside of Nginx).
 *
 * <p>In production Nginx serves {@code /uploads} as a static location
 * (06-testing-deployment.md §8) and this handler is simply never reached.
 * The database stores only the relative storage key (AGENTS.md); the absolute
 * directory is configuration ({@code app.media.storage-dir}).</p>
 */
@Configuration
public class MediaResourceConfig implements WebMvcConfigurer {

    private final String storageDir;

    public MediaResourceConfig(@Value("${app.media.storage-dir:uploads}") String storageDir) {
        this.storageDir = storageDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolute = Path.of(storageDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolute)
                // 30 days: media files are immutable once uploaded.
                .setCachePeriod(2_592_000);
    }
}
