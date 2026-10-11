/*
 * SoftwareDetailValidator：作品模块的输入校验文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 把外部输入转换为可供业务使用的值，并在格式或范围不符合要求时明确失败。
 * 主要协作类型：WorkType、WorkInvalidException、WorkDetailValidator。
 * 阅读入口：supports（判断是否支持Software详情校验器）；validate（校验Software详情校验器）。
 */

package com.starrainnotes.portfolio.validator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.portfolio.enumeration.WorkType;
import com.starrainnotes.portfolio.exception.WorkInvalidException;
import com.starrainnotes.portfolio.validator.WorkDetailValidator;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Software详情校验器，属于作品模块。
 * 把外部输入转换为可供业务使用的值，并在格式或范围不符合要求时明确失败。
 */
@Component
public class SoftwareDetailValidator extends AbstractWorkDetailValidator implements WorkDetailValidator {
    /**
     * 判断是否支持Software详情校验器。
     * @return 类型为 <code>WorkType</code> 的处理结果
     */
    @Override
    public WorkType supports() {
        return WorkType.SOFTWARE;
    }

    /**
     * 校验Software详情校验器。
     * 关键处理步骤调用 <code>fields</code>、<code>Set.of</code>、<code>detail.get</code>、<code>stack.isArray</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(stack == null || !stack.isArray() || stack.isEmpty() || stack.size() &gt; 20)</code>；<code>(!item.isTextual() || item.asText().isBlank() || item.asText().length() &gt; 60)</code>。
     *
     * @param detail 详情；类型为 <code>JsonNode</code>
     */
    @Override
    public void validate(JsonNode detail) {
        fields(detail, Set.of("techStack", "role", "projectStage"));
        JsonNode stack = detail.get("techStack");
        if (stack == null || !stack.isArray() || stack.isEmpty() || stack.size() > 20) {
            throw new WorkInvalidException("techStack 必须包含 1～20 项");
        }
        for (JsonNode item : stack) {
            if (!item.isTextual() || item.asText().isBlank() || item.asText().length() > 60) {
                throw new WorkInvalidException("技术栈名称无效");
            }
        }
        text(detail, "role");
        if (!Set.of("DEVELOPING", "COMPLETED", "ONLINE").contains(text(detail, "projectStage"))) {
            throw new WorkInvalidException("项目阶段无效");
        }
    }
}
