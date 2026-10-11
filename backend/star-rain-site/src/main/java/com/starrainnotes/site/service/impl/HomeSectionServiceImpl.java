/*
 * HomeSectionServiceImpl：站点模块的业务实现文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：HomeSectionOrderDTO、HomeSectionPatchDTO、HomeSectionEntity、HomeSectionMapper、HomeSectionService、HomeSectionSettingVO、SiteConfigException。
 * 阅读入口：all（全部处理入口）；enabled（已启用处理入口）；reorder（调整顺序首页区块）；patch（patch处理入口）。
 */

package com.starrainnotes.site.service.impl;

import com.starrainnotes.site.dto.HomeSectionOrderDTO;
import com.starrainnotes.site.dto.HomeSectionPatchDTO;
import com.starrainnotes.site.entity.HomeSectionEntity;
import com.starrainnotes.site.mapper.HomeSectionMapper;
import com.starrainnotes.site.service.HomeSectionService;
import com.starrainnotes.site.vo.HomeSectionSettingVO;

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

/**
 * 首页区块业务服务实现，属于站点模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class HomeSectionServiceImpl implements HomeSectionService {
    /**
     * CODES。
     */
    public static final Set<String> CODES = Set.of("HERO", "LATEST", "TUTORIALS", "BLOG", "PORTFOLIO", "PROFILE", "HOT_CONTENT");
    /**
     * 数据映射。
     * 协作类型：<code>HomeSectionMapper</code>。
     */
    private final HomeSectionMapper mapper;
    /**
     * JSON 读写或映射协作对象。
     * 协作类型：<code>ObjectMapper</code>。
     */
    private final ObjectMapper json;

    /**
     * 全部处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.all</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    @Transactional(readOnly = true)
    public List<HomeSectionSettingVO> all() {
        return mapper.all().stream().map(HomeSectionSettingVO::from).toList();
    }

    /**
     * 已启用处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.all</code>。
     * @return 本次处理得到的条目集合
     */
    @Override
    @Transactional(readOnly = true)
    public List<HomeSectionEntity> enabled() {
        // 内部聚合用：直接读实体，避免为了拿 config_json 再做一次 VO→实体的反向映射
        return mapper.all().stream().filter(HomeSectionEntity::getEnabled).toList();
    }

    /**
     * 调整顺序首页区块。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.all</code>、<code>mapper.update</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>HomeSectionOrderDTO</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    @Transactional
    public List<HomeSectionSettingVO> reorder(HomeSectionOrderDTO request) {
        List<HomeSectionEntity> rows = mapper.all();
        List<String> codes = request == null ? null : request.getSectionCodes();
        if (codes == null || codes.size() != rows.size() || codes.stream().anyMatch(Objects::isNull)
                || !Set.copyOf(codes).equals(
                rows.stream().map(HomeSectionEntity::getSectionCode).collect(java.util.stream.Collectors.toSet())))
            throw invalid("必须按当前区块完整提交排序");
        for (HomeSectionEntity row : rows) {
            row.setSortOrder(codes.indexOf(row.getSectionCode()) * 10);
            row.setUpdatedAt(LocalDateTime.now());
            mapper.update(row);
        }
        return all();
    }

    /**
     * patch处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>CODES.contains</code>、<code>mapper.byCode</code>、<code>mapper.update</code>。
     * 实现中的显式失败类型包括 <code>SiteConfigException</code>；具体触发条件见对应分支。
     *
     * @param code 代码；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>HomeSectionPatchDTO</code>
     * @return 类型为 <code>HomeSectionSettingVO</code> 的处理结果
     */
    @Override
    @Transactional
    public HomeSectionSettingVO patch(String code, HomeSectionPatchDTO request) {
        if (!CODES.contains(code)) throw new SiteConfigException("SITE_SECTION_CODE_UNSUPPORTED", "不支持的首页区块", 400);
        if (request == null) throw invalid("区块配置不能为空");
        HomeSectionEntity row = mapper.byCode(code);
        if (row == null) throw new SiteConfigException("SITE_SECTION_NOT_FOUND", "首页区块不存在", 404);
        if (request.getDisplayName() != null) {
            String name = request.getDisplayName().trim();
            if (name.isEmpty() || name.length() > 120) throw invalid("区块名称长度无效");
            row.setDisplayName(name);
        }
        if (request.getEnabled() != null) row.setEnabled(request.getEnabled());
        if (request.getConfig() != null) row.setConfigJson(validateConfig(request.getConfig()));
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        return HomeSectionSettingVO.from(row);
    }

    /**
     * 校验配置。
     * 主要协作调用：<code>json.writeValueAsString</code>。
     *
     * @param config 配置；类型为 <code>JsonNode</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String validateConfig(JsonNode config) {
        if (!config.isObject()) throw invalid("区块配置必须是对象");
        var fields = config.fieldNames();
        while (fields.hasNext()) {
            String name = fields.next();
            if (!Set.of("limit", "layout", "selectedIds").contains(name)) throw invalid("区块配置包含不支持的字段");
        }
        JsonNode limit = config.get("limit");
        if (limit != null && (!limit.isIntegralNumber() || !limit.canConvertToInt()
                || limit.intValue() < 1 || limit.intValue() > 12))
            throw invalid("展示数量须在 1 到 12 之间");
        JsonNode layout = config.get("layout");
        if (layout != null && (!layout.isTextual() || !Set.of("cards", "list", "hero").contains(layout.asText())))
            throw invalid("区块布局无效");
        JsonNode selectedIds = config.get("selectedIds");
        if (selectedIds != null) {
            if (!selectedIds.isArray() || selectedIds.size() != 3) throw invalid("精选内容必须选满三项");
            Set<Long> unique = new java.util.HashSet<>();
            for (JsonNode id : selectedIds) {
                if (!id.canConvertToLong() || id.asLong() <= 0 || !unique.add(id.asLong())) {
                    throw invalid("精选内容 ID 必须是三个不重复的正整数");
                }
            }
        }
        try { return json.writeValueAsString(config); }
        catch (JsonProcessingException exception) { throw invalid("区块配置不是有效 JSON"); }
    }

    /**
     * 无效处理入口。
     *
     * @param message 留言；类型为 <code>String</code>
     * @return 类型为 <code>SiteConfigException</code> 的处理结果
     */
    private SiteConfigException invalid(String message) {
        return new SiteConfigException("SITE_SECTION_CONFIG_INVALID", message, 400);
    }
}
