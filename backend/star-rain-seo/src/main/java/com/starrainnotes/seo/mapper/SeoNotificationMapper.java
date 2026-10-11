/*
 * SeoNotificationMapper：公开页面与 SEO模块的数据访问文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：SeoNotificationLog。
 * 阅读入口：enqueue（enqueue处理入口）；due（due处理入口）；claim（claim处理入口）；success（success处理入口）。
 */

package com.starrainnotes.seo.mapper;

import com.starrainnotes.seo.entity.SeoNotificationLog;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SEO通知数据映射，属于公开页面与 SEO模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface SeoNotificationMapper {
    /**
     * enqueue处理入口。
     *
     * 持久化操作：新增，涉及 <code>sr_seo_notification_log</code>。
     *
     * @param log 当前类使用的日志记录器；类型为 <code>SeoNotificationLog</code>
     * @return SQL 写入影响的记录行数
     */
    int enqueue(SeoNotificationLog log);
    /**
     * due处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_seo_notification_log</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<SeoNotificationLog> due(@Param("now") LocalDateTime now, @Param("limit") int limit);
    /**
     * claim处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_seo_notification_log</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param now now；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("now")</code>
     * @return SQL 写入影响的记录行数
     */
    int claim(@Param("id") Long id, @Param("now") LocalDateTime now);
    /**
     * success处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_seo_notification_log</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param httpStatus HTTP状态；类型为 <code>Integer</code>；对应 MyBatis 命名参数 <code>@Param("httpStatus")</code>
     * @return SQL 写入影响的记录行数
     */
    int success(@Param("id") Long id, @Param("httpStatus") Integer httpStatus);
    /**
     * 失败处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_seo_notification_log</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param httpStatus HTTP状态；类型为 <code>Integer</code>；对应 MyBatis 命名参数 <code>@Param("httpStatus")</code>
     * @param error 错误；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("error")</code>
     * @param nextRetryAt 下一项重试时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("nextRetryAt")</code>
     * @return SQL 写入影响的记录行数
     */
    int failure(@Param("id") Long id, @Param("httpStatus") Integer httpStatus,
        @Param("error") String error, @Param("nextRetryAt") LocalDateTime nextRetryAt);
}
