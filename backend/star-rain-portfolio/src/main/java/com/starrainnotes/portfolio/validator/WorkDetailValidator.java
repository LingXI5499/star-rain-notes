/*
 * WorkDetailValidator：作品模块的输入校验文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 把外部输入转换为可供业务使用的值，并在格式或范围不符合要求时明确失败。
 * 主要协作类型：WorkType。
 * 阅读入口：supports（判断是否支持作品详情校验器）；validate（校验作品详情校验器）。
 */

package com.starrainnotes.portfolio.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.portfolio.enumeration.WorkType;

/**
 * 作品详情校验器，属于作品模块。
 * 把外部输入转换为可供业务使用的值，并在格式或范围不符合要求时明确失败。
 */
public interface WorkDetailValidator {
    /**
     * 判断是否支持作品详情校验器。
     * @return 类型为 <code>WorkType</code> 的处理结果
     */
    WorkType supports();

    /**
     * 校验作品详情校验器。
     *
     * @param detail 详情；类型为 <code>JsonNode</code>
     */
    void validate(JsonNode detail);
}
