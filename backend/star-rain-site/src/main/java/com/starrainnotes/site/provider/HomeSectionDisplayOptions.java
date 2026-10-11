/*
 * HomeSectionDisplayOptions：站点模块的模块支持文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：HomeSectionEntity。
 * 阅读入口：limitOf（上限所属处理入口）；layoutOf（layout所属处理入口）；selectedIdsOf（选中标识集合所属处理入口）。
 */

package com.starrainnotes.site.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.site.entity.HomeSectionEntity;
import java.util.Set;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 首页区块显示Options，属于站点模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class HomeSectionDisplayOptions {
    /**
     * JSON 读写或映射协作对象。
     * 协作类型：<code>ObjectMapper</code>。
     */
    private final ObjectMapper json;

    /**
     * 上限所属处理入口。
     * 主要协作调用：<code>json.readTree</code>。
     *
     * @param section 区块；类型为 <code>HomeSectionEntity</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    public int limitOf(HomeSectionEntity section) {
        try {
            JsonNode config = json.readTree(section.getConfigJson());
            int limit = config.path("limit").asInt(6);
            return Math.max(1, Math.min(limit, 12));
        } catch (Exception exception) {
            return 6;
        }
    }

    /**
     * layout所属处理入口。
     * 主要协作调用：<code>json.readTree</code>。
     *
     * @param section 区块；类型为 <code>HomeSectionEntity</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
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

    /**
     * 选中标识集合所属处理入口。
     * 主要协作调用：<code>json.readTree</code>。
     *
     * @param section 区块；类型为 <code>HomeSectionEntity</code>
     * @return 本次处理得到的条目集合
     */
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
