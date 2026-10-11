/*
 * AbstractWorkDetailValidator：作品模块的输入校验文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 把外部输入转换为可供业务使用的值，并在格式或范围不符合要求时明确失败。
 * 主要协作类型：WorkInvalidException。
 * 阅读入口：fields（字段集合处理入口）；text（文本处理入口）；positiveInt（positiveInt处理入口）。
 */

package com.starrainnotes.portfolio.validator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.portfolio.exception.WorkInvalidException;
import java.util.Iterator;
import java.util.Set;

/**
 * Abstract作品详情校验器，属于作品模块。
 * 把外部输入转换为可供业务使用的值，并在格式或范围不符合要求时明确失败。
 */
abstract class AbstractWorkDetailValidator {
    /**
     * 字段集合处理入口。
     * 关键处理步骤调用 <code>detail.isObject</code>、<code>detail.fieldNames</code>、<code>names.hasNext</code>、<code>allowed.contains</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(detail == null || !detail.isObject())</code>；<code>(!allowed.contains(names.next()))</code>。
     *
     * @param detail 详情；类型为 <code>JsonNode</code>
     * @param allowed allowed；类型为 <code>Set&lt;String&gt;</code>
     */
    void fields(JsonNode detail, Set<String> allowed) {
        if (detail == null || !detail.isObject()) {
            throw new WorkInvalidException("类型详情必须是对象");
        }
        Iterator<String> names = detail.fieldNames();
        while (names.hasNext()) {
            if (!allowed.contains(names.next())) {
                throw new WorkInvalidException("类型详情包含不支持的字段");
            }
        }
    }

    /**
     * 文本处理入口。
     * 关键处理步骤调用 <code>detail.get</code>、<code>value.isTextual</code>、<code>value.asText</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(value == null || !value.isTextual() || value.asText().isBlank() || value.asText().length() &gt; 200)</code>。
     *
     * @param detail 详情；类型为 <code>JsonNode</code>
     * @param field 字段；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    String text(JsonNode detail, String field) {
        JsonNode value = detail.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()
                || value.asText().length() > 200) {
            throw new WorkInvalidException(field + " 必须是 1～200 字的文本");
        }
        return value.asText().trim();
    }

    /**
     * positiveInt处理入口。
     * 关键处理步骤调用 <code>detail.get</code>、<code>value.isIntegralNumber</code>、<code>value.asLong</code>、<code>value.asInt</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(value == null || !value.isIntegralNumber() || value.asLong() &lt; 1 || value.asLong() &gt; max)</code>。
     *
     * @param detail 详情；类型为 <code>JsonNode</code>
     * @param field 字段；类型为 <code>String</code>
     * @param max 最大；类型为 <code>int</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    int positiveInt(JsonNode detail, String field, int max) {
        JsonNode value = detail.get(field);
        if (value == null || !value.isIntegralNumber() || value.asLong() < 1 || value.asLong() > max) {
            throw new WorkInvalidException(field + " 必须是有效正整数");
        }
        return value.asInt();
    }
}
