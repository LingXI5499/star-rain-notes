/*
 * WorkMediaDTO：作品模块的请求与传输数据文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 * 主要协作类型：WorkMediaUsage。
 */

package com.starrainnotes.portfolio.dto;

import com.starrainnotes.portfolio.enumeration.WorkMediaUsage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作品媒体传输对象，属于作品模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkMediaDTO {
    /**
     * 关联的媒体资产标识。
     * 不允许为 null。
     */
    @NotNull
    private Long mediaAssetId;
    /**
     * usage类型。
     * 不允许为 null。
     */
    @NotNull
    private WorkMediaUsage usageType;
    /**
     * 说明。
     * 校验规则 <code>@Size(max = 500)</code>。
     */
    @Size(max = 500)
    private String caption;
    /**
     * 展示排序值。
     */
    private Integer sortOrder;
}
