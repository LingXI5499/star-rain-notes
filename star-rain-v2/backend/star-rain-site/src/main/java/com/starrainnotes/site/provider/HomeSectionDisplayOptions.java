package com.starrainnotes.site.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.site.entity.HomeSectionEntity;
import java.util.Set;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HomeSectionDisplayOptions {
    private final ObjectMapper json;

    public int limitOf(HomeSectionEntity section) {
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

    public List<Long> selectedIdsOf(HomeSectionEntity section) {
        try {
            JsonNode ids = json.readTree(section.getConfigJson()).path("selectedIds");
            if (!ids.isArray()) return List.of();
            List<Long> result = new java.util.ArrayList<>();
            ids.forEach(id -> { if (id.canConvertToLong() && id.asLong() > 0) result.add(id.asLong()); });
            return result;
        } catch (Exception exception) {
            return List.of();
        }
    }
}
