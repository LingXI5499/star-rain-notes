package com.starrainnotes.seo.renderer;

import com.starrainnotes.seo.config.SeoProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeoInteractiveAssetRenderer {
    private static final Logger log = LoggerFactory.getLogger(SeoInteractiveAssetRenderer.class);
    private static final Pattern ASSET = Pattern.compile(
        "(?is)<script\\b[^>]*\\bsrc=\"/assets/[^\"]+\"[^>]*></script>|<link\\b[^>]*\\bhref=\"/assets/[^\"]+\"[^>]*>");
    private final SeoProperties properties;
    private volatile AssetEntry cached;

    public String include(String html) {
        String path = properties.getFrontendIndexPath();
        if (path == null || path.isBlank()) return html;
        StringBuilder assets = new StringBuilder();
        for (String tag : assetTags(path)) {
            if (!html.contains(tag)) assets.append(tag);
        }
        return html.replace("</head>", assets + "</head>");
    }

    private synchronized List<String> assetTags(String configuredPath) {
        Path path = Path.of(configuredPath).toAbsolutePath().normalize();
        String version = path + ":missing";
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            version = path + ":" + attributes.lastModifiedTime() + ":" + attributes.size() + ":" + attributes.fileKey();
            if (cached != null && cached.version.equals(version)) return cached.tags;
            if (attributes.size() > 1_048_576) throw new IOException("Frontend index exceeds size limit");
            String index = Files.readString(path, StandardCharsets.UTF_8);
            Matcher matcher = ASSET.matcher(index);
            var tags = new LinkedHashSet<String>();
            while (matcher.find()) tags.add(matcher.group());
            cached = new AssetEntry(version, List.copyOf(tags));
        } catch (IOException exception) {
            if (cached == null || !cached.version.equals(version)) {
                log.warn("Frontend build index unavailable for SEO HTML; serving semantic snapshot");
                cached = new AssetEntry(version, List.of());
            }
        }
        return cached.tags;
    }

    private static final class AssetEntry {
        private final String version;
        private final List<String> tags;
        private AssetEntry(String version, List<String> tags) {
            this.version = version;
            this.tags = tags;
        }
    }
}
