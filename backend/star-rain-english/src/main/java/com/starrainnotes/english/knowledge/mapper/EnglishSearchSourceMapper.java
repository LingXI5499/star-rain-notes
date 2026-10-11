/*
 * EnglishSearchSourceMapper：英语学习模块的数据访问文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：EnglishSearchDocument。
 * 阅读入口：page（分页读取英语搜索来源）；findPublished（查找已发布）。
 */

package com.starrainnotes.english.knowledge.mapper;

import com.starrainnotes.english.api.dto.EnglishSearchDocument;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 英语搜索来源数据映射，属于英语学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface EnglishSearchSourceMapper {
    /**
     * 分页读取英语搜索来源。
     *
     * 持久化语句类型：<code>select</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param type 类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("type")</code>
     * @param afterId 之后标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("afterId")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<EnglishSearchDocument> page(@Param("type") String type, @Param("afterId") Long afterId,
        @Param("limit") int limit);

    /**
     * 查找已发布。
     *
     * 持久化语句类型：<code>select</code>。
     *
     * @param type 类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("type")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>EnglishSearchDocument</code> 的处理结果
     */
    EnglishSearchDocument findPublished(@Param("type") String type, @Param("id") Long id);
}
