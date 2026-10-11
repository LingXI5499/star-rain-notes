/*
 * MediaQueryDTO：媒体模块的请求与传输数据文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 * 主要协作类型：MediaType。
 */

package com.starrainnotes.media.dto;

import com.starrainnotes.media.enumeration.MediaType;
import lombok.Data;

/*
 * MED-002 查询条件。
 *
 * 这里刻意不加 jakarta.validation 注解：
 * 查询接口的合法性由 Service 显式校验并返回 MEDIA_QUERY_INVALID，
 * 避免依赖 @ModelAttribute 的绑定异常类型，也让错误码保持可控、可测。
 */
/**
 * 媒体查询参数传输对象，属于媒体模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class MediaQueryDTO {

    /**
     * 分页页码。
     * 初始值为 <code>1</code>。
     */
    private int page = 1;

    /**
     * 单页返回条目数。
     * 初始值为 <code>20</code>。
     */
    private int pageSize = 20;

    // 按原始文件名模糊匹配
    /**
     * 关键词。
     */
    private String keyword;

    // IMAGE / DOCUMENT / AUDIO / OTHER
    /**
     * 媒体类型。
     */
    private String mediaType;

    // ACTIVE / ARCHIVED
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;

    // PUBLIC / PROTECTED
    /**
     * 媒体访问级别。
     */
    private String accessLevel;
}
