/*
 * PageResult：公共基础模块的模块支持文件。
 * 业务范围：统一响应、异常、分页、安全与跨模块基础契约。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */

package com.starrainnotes.common.result;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页结果，属于公共基础模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /**
     * 条目集合。
     */
    private List<T> items;
    /**
     * 符合查询条件的总记录数。
     */
    private long total;
    /**
     * 分页页码。
     */
    private int page;
    /**
     * 单页返回条目数。
     */
    private int pageSize;
}
