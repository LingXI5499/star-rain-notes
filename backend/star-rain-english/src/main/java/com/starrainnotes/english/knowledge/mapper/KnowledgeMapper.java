/*
 * KnowledgeMapper：英语学习模块的数据访问文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：Item。
 * 阅读入口：count（统计知识）；list（列出知识）。
 */

package com.starrainnotes.english.knowledge.mapper;
import com.starrainnotes.english.knowledge.dto.KnowledgeDto.Item;
import java.util.List;
import org.apache.ibatis.annotations.Param;
/**
 * 知识数据映射，属于英语学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface KnowledgeMapper {
    /**
     * 统计知识。
     *
     * 持久化语句类型：<code>select</code>。
     *
     * @param type 类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("type")</code>
     * @param search 搜索；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("search")</code>
     * @param topics 专题列表；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("topics")</code>
     * @param genres genres；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("genres")</code>
     * @param purposes purposes；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("purposes")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long count(@Param("type") String type,@Param("search") String search,@Param("topics") List<Long> topics,@Param("genres") List<Long> genres,@Param("purposes") List<Long> purposes);
    /**
     * 列出知识。
     *
     * 持久化语句类型：<code>select</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param type 类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("type")</code>
     * @param search 搜索；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("search")</code>
     * @param topics 专题列表；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("topics")</code>
     * @param genres genres；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("genres")</code>
     * @param purposes purposes；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("purposes")</code>
     * @param offset 查询跳过的记录数；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<Item> list(@Param("type") String type,@Param("search") String search,@Param("topics") List<Long> topics,@Param("genres") List<Long> genres,@Param("purposes") List<Long> purposes,@Param("offset") long offset,@Param("limit") int limit);
}
