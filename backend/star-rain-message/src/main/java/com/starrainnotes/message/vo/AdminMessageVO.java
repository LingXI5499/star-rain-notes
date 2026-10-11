/*
 * AdminMessageVO：留言模块的响应数据文件。
 * 业务范围：留言提交、审核展示和后台管理。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.message.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 后台留言响应对象，属于留言模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@AllArgsConstructor
public class AdminMessageVO {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
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
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 提交时间。
     */
    private LocalDateTime submittedAt;
    /**
     * moderated时间。
     */
    private LocalDateTime moderatedAt;
    /**
     * reject原因。
     */
    private String rejectReason;
}
