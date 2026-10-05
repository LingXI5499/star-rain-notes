package com.starrainnotes.site.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.site.entity.HomeSectionEntity;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HomeSectionLimit {
    private final ObjectMapper json;

    public int of(HomeSectionEntity section) {
        try {
            JsonNode config = json.readTree(section.getConfigJson());
            int limit = config.path("limit").asInt(6);
            return Math.max(1, Math.min(limit, 12));
        } catch (Exception exception) {
            return 6;
        }
    }

    public String layoutOf(HomeSectionEntity section) {
        String fallback = "HERO".equals(section.getSectionCode()) ? "hero"
                : Set.of("BLOG", "LATEST").contains(section.getSectionCode()) ? "list" : "cards";
        try {
            JsonNode config = json.readTree(section.getConfigJson());
            return config.path("layout").asText(fallback);
        } catch (Exception exception) {
            return fallback;
        }
    }
}
