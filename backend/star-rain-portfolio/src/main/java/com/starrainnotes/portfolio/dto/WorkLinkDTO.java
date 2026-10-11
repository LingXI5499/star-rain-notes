/*
 * WorkLinkDTO：作品模块的请求与传输数据文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作品链接传输对象，属于作品模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkLinkDTO {
    /**
     * 链接类型。
     * 不能为空或仅含空白字符。
     * 校验规则 <code>@Size(max = 30)</code>。
     */
    @NotBlank
    @Size(max = 30)
    private String linkType;
    /**
     * 标签。
     * 不能为空或仅含空白字符。
     * 校验规则 <code>@Size(max = 100)</code>。
     */
    @NotBlank
    @Size(max = 100)
    private String label;
    /**
     * 地址。
     * 不能为空或仅含空白字符。
     * 校验规则 <code>@Size(max = 1000)</code>。
     */
    @NotBlank
    @Size(max = 1000)
    private String url;
    /**
     * 展示排序值。
     */
    private Integer sortOrder;
    /**
     * 已启用。
     */
    private Boolean enabled;
}
