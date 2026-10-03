package com.starrainnotes.tutorial.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 仅登记本站媒体地址；外部图床不属于本站媒体引用。
public final class TutorialContentMediaParser {
    private static final Pattern CONTENT_URL =
            Pattern.compile("(?<![A-Za-z0-9.\\-])/api/media/assets/(\\d+)/content");

    private TutorialContentMediaParser() {
    }

    public static List<Long> extractMediaAssetIds(String markdown) {
        if (markdown == null || markdown.isBlank()) return List.of();
        Set<Long> ids = new LinkedHashSet<>();
        Matcher matcher = CONTENT_URL.matcher(markdown);
        while (matcher.find()) {
            try {
                long id = Long.parseLong(matcher.group(1));
                if (id > 0) ids.add(id);
            } catch (NumberFormatException ignored) {
                // 正文里的超长无效地址不应妨碍保存章节。
            }
        }
        return new ArrayList<>(ids);
    }
}
