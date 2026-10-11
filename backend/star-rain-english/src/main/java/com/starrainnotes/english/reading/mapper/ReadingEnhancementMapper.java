/*
 * ReadingEnhancementMapper：英语学习模块的数据访问文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：Item。
 * 阅读入口：list（列出阅读Enhancement）；item（item处理入口）；touch（touch处理入口）；stale（stale处理入口）。
 */

package com.starrainnotes.english.reading.mapper;
import com.starrainnotes.english.reading.dto.ReadingEnhancementDto.Item;
import java.util.List;
import org.apache.ibatis.annotations.Param;
/**
 * 阅读Enhancement数据映射，属于英语学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface ReadingEnhancementMapper {
    /**
     * 列出阅读Enhancement。
     *
     * 持久化语句类型：<code>select</code>。
     *
     * @param kind kind；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("kind")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("articleId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Item> list(@Param("kind") String kind,@Param("articleId") long id);
    /**
     * item处理入口。
     *
     * 持久化语句类型：<code>select</code>。
     *
     * @param kind kind；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("kind")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("articleId")</code>
     * @param item item；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>Item</code> 的处理结果
     */
    Item item(@Param("kind") String kind,@Param("articleId") long id,@Param("id") long item);
    /**
     * touch处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_english_reading_article</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("articleId")</code>
     * @param version 版本；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("version")</code>
     * @return SQL 写入影响的记录行数
     */
    int touch(@Param("articleId") long id,@Param("version") long version);
    /**
     * stale处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_english_reading_alignment</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("articleId")</code>
     * @param language language；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("language")</code>
     */
    void stale(@Param("articleId") long id,@Param("language") String language);
    /**
     * staleAnnotations处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_english_reading_annotation</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("articleId")</code>
     */
    void staleAnnotations(@Param("articleId") long id);
    /**
     * 清理阅读Enhancement。
     *
     * 持久化语句类型：<code>delete</code>。
     *
     * @param kind kind；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("kind")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("articleId")</code>
     */
    void clear(@Param("kind") String kind,@Param("articleId") long id);
    /**
     * 写入阅读Enhancement。
     *
     * 持久化语句类型：<code>insert</code>。
     *
     * @param kind kind；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("kind")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("articleId")</code>
     * @param item item；类型为 <code>Item</code>；对应 MyBatis 命名参数 <code>@Param("item")</code>
     */
    void insert(@Param("kind") String kind,@Param("articleId") long id,@Param("item") Item item);
    /**
     * 更新阅读Enhancement。
     *
     * 持久化语句类型：<code>update</code>。
     *
     * @param kind kind；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("kind")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("articleId")</code>
     * @param item item；类型为 <code>Item</code>；对应 MyBatis 命名参数 <code>@Param("item")</code>
     * @return SQL 写入影响的记录行数
     */
    int update(@Param("kind") String kind,@Param("articleId") long id,@Param("item") Item item);
    /**
     * 删除阅读Enhancement。
     *
     * 持久化语句类型：<code>delete</code>。
     *
     * @param kind kind；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("kind")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("articleId")</code>
     * @param item item；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int delete(@Param("kind") String kind,@Param("articleId") long id,@Param("id") long item);
}
