/*
 * MessageSubmitDTO：留言模块的请求与传输数据文件。
 * 业务范围：留言提交、审核展示和后台管理。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.message.dto;

import lombok.Data;

/**
 * 留言Submit传输对象，属于留言模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class MessageSubmitDTO {
    /**
     * 作者显示名称。
     */
    private String authorDisplayName;
    /**
     * contact邮箱。
     */
    private String contactEmail;
    /**
     * 内容。
     */
    private String content;
}
