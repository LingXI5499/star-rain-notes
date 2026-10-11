/*
 * ProfileMediaDTO：作者资料模块的请求与传输数据文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.profile.dto;

import lombok.Data;

/**
 * 资料媒体传输对象，属于作者资料模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class ProfileMediaDTO {
    /**
     * 关联的媒体资产标识。
     */
    private Long mediaAssetId;
}
