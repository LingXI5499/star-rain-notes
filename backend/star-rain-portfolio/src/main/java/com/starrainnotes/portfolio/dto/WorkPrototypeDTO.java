/*
 * WorkPrototypeDTO：作品模块的请求与传输数据文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.portfolio.dto;
import lombok.Data;
/**
 * 作品Prototype传输对象，属于作品模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class WorkPrototypeDTO {
    /**
     * 关联的媒体资产标识。
     */
    private Long mediaAssetId;
    /**
     * 入口路径。
     */
    private String entryPath;
}
