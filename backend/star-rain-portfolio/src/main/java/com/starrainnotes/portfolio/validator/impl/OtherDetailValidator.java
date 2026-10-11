/*
 * OtherDetailValidator：作品模块的输入校验文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 把外部输入转换为可供业务使用的值，并在格式或范围不符合要求时明确失败。
 * 主要协作类型：WorkType、WorkDetailValidator。
 * 阅读入口：supports（判断是否支持Other详情校验器）；validate（校验Other详情校验器）。
 */

package com.starrainnotes.portfolio.validator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.portfolio.enumeration.WorkType;
import com.starrainnotes.portfolio.validator.WorkDetailValidator;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Other详情校验器，属于作品模块。
 * 把外部输入转换为可供业务使用的值，并在格式或范围不符合要求时明确失败。
 */
@Component
public class OtherDetailValidator extends AbstractWorkDetailValidator implements WorkDetailValidator {
    /**
     * 判断是否支持Other详情校验器。
     * @return 类型为 <code>WorkType</code> 的处理结果
     */
    @Override
    public WorkType supports() {
        return WorkType.OTHER;
    }

    /**
     * 校验Other详情校验器。
     * 关键处理步骤调用 <code>fields</code>、<code>Set.of</code>、<code>detail.has</code>、<code>text</code>。
     * 边界判断包含 <code>(detail.has("description"))</code>。
     *
     * @param detail 详情；类型为 <code>JsonNode</code>
     */
    @Override
    public void validate(JsonNode detail) {
        fields(detail, Set.of("description"));
        if (detail.has("description")) {
            text(detail, "description");
        }
    }
}
