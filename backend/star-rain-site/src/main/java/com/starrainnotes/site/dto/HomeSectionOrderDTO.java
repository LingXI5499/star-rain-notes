/*
 * HomeSectionOrderDTO：站点模块的请求与传输数据文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.site.dto;

import java.util.List;
import lombok.Data;

/**
 * 首页区块顺序传输对象，属于站点模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class HomeSectionOrderDTO {
    /**
     * 区块Codes。
     */
    private List<String> sectionCodes;
}
