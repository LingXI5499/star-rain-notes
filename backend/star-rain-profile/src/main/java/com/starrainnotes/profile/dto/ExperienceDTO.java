/*
 * ExperienceDTO：作者资料模块的请求与传输数据文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.profile.dto;

import java.time.LocalDate;
import lombok.Data;

/**
 * Experience传输对象，属于作者资料模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class ExperienceDTO {
    /**
     * experience类型。
     */
    private String experienceType;
    /**
     * 标题。
     */
    private String title;
    /**
     * organization。
     */
    private String organization;
    /**
     * 开始日期。
     */
    private LocalDate startDate;
    /**
     * 结束日期。
     */
    private LocalDate endDate;
    /**
     * 是否当前。
     */
    private Boolean isCurrent;
    /**
     * 描述Md。
     */
    private String descriptionMd;
}
