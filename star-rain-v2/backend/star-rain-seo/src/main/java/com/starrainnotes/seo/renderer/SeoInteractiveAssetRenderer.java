package com.starrainnotes.seo.renderer;

import com.starrainnotes.seo.config.SeoProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public String include(String html) {
        String path = properties.getFrontendIndexPath();
        if (path == null || path.isBlank()) return html;
        try {
            String index = Files.readString(Path.of(path), StandardCharsets.UTF_8);
            Matcher matcher = ASSET.matcher(index);
            StringBuilder assets = new StringBuilder();
            while (matcher.find()) assets.append(matcher.group());
            return html.replace("</head>", assets + "</head>");
        } catch (IOException exception) {
            log.debug("Frontend build index unavailable for SEO HTML; serving semantic snapshot");
            return html;
        }
    }
}
