package com.starrainnotes.portfolio.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.portfolio.dto.WorkSectionDTO;
import com.starrainnotes.portfolio.exception.WorkInvalidException;
import com.starrainnotes.portfolio.utils.WorkLinkRules;
import java.util.HashSet;
import java.util.Set;

/** Version-one block schema. Unknown fields are rejected rather than stored as unvalidated HTML. */
public final class WorkSectionRules {
    public static final Set<String> TYPES = Set.of("MARKDOWN", "IMAGE", "GALLERY", "AUDIO", "CODE",
            "TIMELINE", "LINKS", "QUOTE", "FEATURE_LIST", "TECH_STACK", "STATS", "CUSTOM");
    private WorkSectionRules() { }

    public static void validate(WorkSectionDTO request) {
        if (request == null || request.getSectionType() == null || !TYPES.contains(request.getSectionType())) {
            invalid("区块类型无效");
        }
        if (request.getBlockVersion() != null && request.getBlockVersion() != 1) {
            invalid("不支持的区块版本");
        }
        text(request.getTitle(), 255);
        text(request.getContent(), 1_000_000);
        JsonNode data = request.getData();
        if (data != null && (!data.isObject() || data.toString().length() > 100_000)) {
            invalid("区块结构无效或过大");
        }
        if (data == null) { return; }
        switch (request.getSectionType()) {
            case "CODE" -> {
                fields(data, Set.of("language"));
                if (data.has("language") && (!data.get("language").isTextual()
                        || !data.get("language").asText().matches("[a-zA-Z0-9_+-]{0,50}"))) {
                    invalid("代码语言无效");
                }
            }
            case "QUOTE" -> { fields(data, Set.of("author")); value(data, "author", 255); }
            case "TIMELINE", "LINKS", "FEATURE_LIST", "TECH_STACK", "STATS" -> {
                fields(data, Set.of("items"));
                JsonNode items = data.get("items");
                if (items == null) { return; }
                if (!items.isArray() || items.size() > 100) { invalid("条目数量或结构无效"); }
                Set<String> keys = switch (request.getSectionType()) {
                    case "TIMELINE" -> Set.of("date", "title", "description");
                    case "LINKS" -> Set.of("label", "url", "description");
                    case "FEATURE_LIST" -> Set.of("title", "description");
                    case "TECH_STACK" -> Set.of("name", "group");
                    default -> Set.of("label", "value");
                };
                for (JsonNode item : items) {
                    if (!item.isObject()) { invalid("条目结构无效"); }
                    fields(item, keys);
                    for (String key : keys) { value(item, key, 2000); }
                    if ("LINKS".equals(request.getSectionType())) {
                        WorkLinkRules.requireSafeUrl(item.path("url").asText(null));
                    }
                }
            }
            default -> fields(data, Set.of());
        }
    }

    public static void complete(WorkSectionDTO request) {
        validate(request);
        String type = request.getSectionType();
        if (Set.of("IMAGE", "GALLERY", "AUDIO").contains(type)) {
            int size = request.getMedia() == null ? 0 : request.getMedia().size();
            if (size == 0 || (!"GALLERY".equals(type) && size != 1)) {
                invalid("图片 / 音频区块需要对应媒体");
            }
        } else if (Set.of("TIMELINE", "LINKS", "FEATURE_LIST", "TECH_STACK", "STATS").contains(type)) {
            if (request.getData() == null || request.getData().path("items").isEmpty()) {
                invalid("可见结构区块需要至少一个条目");
            }
            String required = switch (type) {
                case "LINKS", "STATS" -> "label";
                case "TECH_STACK" -> "name";
                default -> "title";
            };
            for (JsonNode item : request.getData().path("items")) {
                if (item.path(required).asText().isBlank()
                        || ("STATS".equals(type) && item.path("value").asText().isBlank())) {
                    invalid("可见区块的条目名称 / 数值不能留空");
                }
            }
        } else if (request.getContent() == null || request.getContent().isBlank()) {
            invalid("可见正文区块不能留空");
        }
    }

    private static void fields(JsonNode node, Set<String> allowed) {
        Set<String> keys = new HashSet<>();
        node.fieldNames().forEachRemaining(keys::add);
        if (!allowed.containsAll(keys)) { invalid("区块包含未支持的字段"); }
    }
    private static void value(JsonNode node, String key, int max) {
        if (!node.has(key)) { return; }
        if (!node.get(key).isTextual()) { invalid("条目字段必须是文本"); }
        text(node.get(key).asText(), max);
    }
    private static void text(String value, int max) {
        if (value != null && value.length() > max) { invalid("区块内容过长"); }
    }
    private static void invalid(String message) { throw new WorkInvalidException(message); }
}
