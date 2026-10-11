/*
 * PublicMessageVO：留言模块的响应数据文件。
 * 业务范围：留言提交、审核展示和后台管理。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.message.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 公开留言响应对象，属于留言模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@AllArgsConstructor
public class PublicMessageVO {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 作者显示名称。
     */
    private String authorDisplayName;
    /**
     * 内容。
     */
    private String content;
    /**
     * 提交时间。
     */
    private LocalDateTime submittedAt;
}
