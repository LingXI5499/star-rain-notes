package com.starrainnotes.site.section;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.site.exception.SiteConfigException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeSectionService {
    public static final Set<String> CODES = Set.of("HERO", "TUTORIALS", "BLOG", "PORTFOLIO", "PROFILE", "HOT_CONTENT");
    private final HomeSectionMapper mapper;
    private final ObjectMapper json;

    @Transactional(readOnly = true)
    public List<HomeSectionEntity> all() {
        return mapper.selectList(Wrappers.<HomeSectionEntity>lambdaQuery()
                .orderByAsc(HomeSectionEntity::getSortOrder, HomeSectionEntity::getId));
    }

    @Transactional(readOnly = true)
    public List<HomeSectionEntity> enabled() {
        return all().stream().filter(HomeSectionEntity::getEnabled).toList();
    }

    @Transactional
    public List<HomeSectionEntity> reorder(HomeSectionOrder request) {
        List<HomeSectionEntity> rows = all();
        List<String> codes = request == null ? null : request.getSectionCodes();
        if (codes == null || codes.size() != rows.size() || codes.stream().anyMatch(Objects::isNull)
                || !Set.copyOf(codes).equals(
                rows.stream().map(HomeSectionEntity::getSectionCode).collect(java.util.stream.Collectors.toSet())))
            throw invalid("必须按当前区块完整提交排序");
        for (HomeSectionEntity row : rows) {
            row.setSortOrder(codes.indexOf(row.getSectionCode()) * 10);
            row.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(row);
        }
        return all();
    }

    @Transactional
    public HomeSectionEntity patch(String code, HomeSectionPatch request) {
        if (!CODES.contains(code)) throw new SiteConfigException("SITE_SECTION_CODE_UNSUPPORTED", "不支持的首页区块", 400);
        if (request == null) throw invalid("区块配置不能为空");
        HomeSectionEntity row = mapper.selectOne(Wrappers.<HomeSectionEntity>lambdaQuery()
                .eq(HomeSectionEntity::getSectionCode, code));
        if (row == null) throw new SiteConfigException("SITE_SECTION_NOT_FOUND", "首页区块不存在", 404);
        if (request.getDisplayName() != null) {
            String name = request.getDisplayName().trim();
            if (name.isEmpty() || name.length() > 120) throw invalid("区块名称长度无效");
            row.setDisplayName(name);
        }
        if (request.getEnabled() != null) row.setEnabled(request.getEnabled());
        if (request.getConfig() != null) row.setConfigJson(validateConfig(request.getConfig()));
        row.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(row);
        return row;
    }

    private String validateConfig(JsonNode config) {
        if (!config.isObject()) throw invalid("区块配置必须是对象");
        var fields = config.fieldNames();
        while (fields.hasNext()) {
            String name = fields.next();
            if (!Set.of("limit", "layout").contains(name)) throw invalid("区块配置包含不支持的字段");
        }
        JsonNode limit = config.get("limit");
        if (limit != null && (!limit.isIntegralNumber() || !limit.canConvertToInt()
                || limit.intValue() < 1 || limit.intValue() > 12))
            throw invalid("展示数量须在 1 到 12 之间");
        JsonNode layout = config.get("layout");
        if (layout != null && (!layout.isTextual() || !Set.of("cards", "list", "hero").contains(layout.asText())))
            throw invalid("区块布局无效");
        try { return json.writeValueAsString(config); }
        catch (JsonProcessingException exception) { throw invalid("区块配置不是有效 JSON"); }
    }

    private SiteConfigException invalid(String message) {
        return new SiteConfigException("SITE_SECTION_CONFIG_INVALID", message, 400);
    }
}
