/*
 * ReadingMapper：英语学习模块的数据访问文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：Article、Rights。
 * 阅读入口：count（统计阅读）；list（列出阅读）；byId（依据标识处理入口）；lockById（按标识读取锁）。
 */

package com.starrainnotes.english.reading.mapper;
import com.starrainnotes.english.reading.dto.ReadingDto.Article;
import com.starrainnotes.english.reading.dto.ReadingDto.Rights;
import java.util.List;
import org.apache.ibatis.annotations.Param;
/**
 * 阅读数据映射，属于英语学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface ReadingMapper {
    /**
     * 统计阅读。
     *
     * 持久化操作：读取，涉及 <code>sr_english_reading_article</code>。
     *
     * @param publicOnly 公开Only；类型为 <code>boolean</code>；对应 MyBatis 命名参数 <code>@Param("publicOnly")</code>
     * @param search 搜索；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("search")</code>
     * @param topics 专题列表；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("topics")</code>
     * @param genres genres；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("genres")</code>
     * @param purposes purposes；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("purposes")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long count(@Param("publicOnly") boolean publicOnly,@Param("search") String search,@Param("topics") List<Long> topics,@Param("genres") List<Long> genres,@Param("purposes") List<Long> purposes);
    /**
     * 列出阅读。
     *
     * 持久化操作：读取，涉及 <code>sr_english_reading_article</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param publicOnly 公开Only；类型为 <code>boolean</code>；对应 MyBatis 命名参数 <code>@Param("publicOnly")</code>
     * @param search 搜索；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("search")</code>
     * @param topics 专题列表；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("topics")</code>
     * @param genres genres；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("genres")</code>
     * @param purposes purposes；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("purposes")</code>
     * @param offset 查询跳过的记录数；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<Article> list(@Param("publicOnly") boolean publicOnly,@Param("search") String search,@Param("topics") List<Long> topics,@Param("genres") List<Long> genres,@Param("purposes") List<Long> purposes,@Param("offset") long offset,@Param("limit") int limit);
    /**
     * 依据标识处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_reading_article</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article byId(@Param("id") long id);
    /**
     * 按标识读取锁。
     *
     * 持久化操作：读取，涉及 <code>sr_english_reading_article</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article lockById(@Param("id") long id);
    /**
     * 按固定链接标识读取公开。
     *
     * 持久化操作：读取，涉及 <code>sr_english_reading_article</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article publicBySlug(@Param("slug") String slug);
    /**
     * 写入阅读。
     *
     * 持久化操作：新增，涉及 <code>sr_english_reading_article</code>。
     *
     * @param article article；类型为 <code>Article</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(Article article);
    /**
     * finish固定链接标识处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_english_reading_article</code>。
     *
     * @param article article；类型为 <code>Article</code>
     * @return SQL 写入影响的记录行数
     */
    int finishSlug(Article article);
    /**
     * 更新阅读。
     *
     * 持久化操作：更新，涉及 <code>sr_english_reading_article</code>。
     *
     * @param article article；类型为 <code>Article</code>
     * @return SQL 写入影响的记录行数
     */
    int update(Article article);
    /**
     * 设置状态。
     *
     * 持久化操作：更新，涉及 <code>sr_english_reading_article</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param version 版本；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("version")</code>
     * @return SQL 写入影响的记录行数
     */
    int setStatus(@Param("id") long id,@Param("status") String status,@Param("version") long version);
    /**
     * 删除阅读。
     *
     * 持久化操作：更新，涉及 <code>sr_english_reading_article</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param version 版本；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("version")</code>
     * @return SQL 写入影响的记录行数
     */
    int delete(@Param("id") long id,@Param("version") long version);
    /**
     * rights处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_reading_rights</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>Rights</code> 的处理结果
     */
    Rights rights(@Param("id") long id);
    /**
     * 保存Rights。
     *
     * 持久化操作：新增，涉及 <code>sr_english_reading_rights</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param rights rights；类型为 <code>Rights</code>；对应 MyBatis 命名参数 <code>@Param("rights")</code>
     * @param sourceName 来源名称；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("sourceName")</code>
     * @param sourceUrl 来源地址；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("sourceUrl")</code>
     */
    void saveRights(@Param("id") long id,@Param("rights") Rights rights,@Param("sourceName") String sourceName,@Param("sourceUrl") String sourceUrl);
}
