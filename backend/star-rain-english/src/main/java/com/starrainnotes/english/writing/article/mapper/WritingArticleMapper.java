/*
 * WritingArticleMapper：英语学习模块的数据访问文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：Article。
 * 阅读入口：count（统计写作Article）；list（列出写作Article）；owned（owned处理入口）；locked（locked处理入口）。
 */

package com.starrainnotes.english.writing.article.mapper;
import com.starrainnotes.english.writing.article.dto.WritingArticleDto.Article;
import java.util.List;
import org.apache.ibatis.annotations.Param;
/**
 * 写作Article数据映射，属于英语学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface WritingArticleMapper {
    /**
     * 统计写作Article。
     *
     * 持久化操作：读取，涉及 <code>sr_english_writing_article</code>。
     *
     * @param owner owner；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("owner")</code>
     * @param search 搜索；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("search")</code>
     * @param state 状态；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("state")</code>
     * @param topics 专题列表；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("topics")</code>
     * @param genres genres；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("genres")</code>
     * @param purposes purposes；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("purposes")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long count(@Param("owner") Long owner,@Param("search") String search,@Param("state") String state,@Param("topics") List<Long> topics,@Param("genres") List<Long> genres,@Param("purposes") List<Long> purposes);
    /**
     * 列出写作Article。
     *
     * 持久化操作：读取，涉及 <code>sr_english_writing_article</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param owner owner；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("owner")</code>
     * @param search 搜索；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("search")</code>
     * @param state 状态；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("state")</code>
     * @param topics 专题列表；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("topics")</code>
     * @param genres genres；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("genres")</code>
     * @param purposes purposes；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("purposes")</code>
     * @param offset 查询跳过的记录数；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<Article> list(@Param("owner") Long owner,@Param("search") String search,@Param("state") String state,@Param("topics") List<Long> topics,@Param("genres") List<Long> genres,@Param("purposes") List<Long> purposes,@Param("offset") long offset,@Param("limit") int limit);
    /**
     * owned处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_writing_article</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param owner owner；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("owner")</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article owned(@Param("id") long id,@Param("owner") long owner);
    /**
     * locked处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_writing_article</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param owner owner；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("owner")</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article locked(@Param("id") long id,@Param("owner") long owner);
    /**
     * 按固定链接标识读取公开。
     *
     * 持久化操作：读取，涉及 <code>sr_english_writing_article</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article publicBySlug(String slug);
    Article lockAny(@Param("id") long id);
    /**
     * 写入写作Article。
     *
     * 持久化操作：新增，涉及 <code>sr_english_writing_article</code>。
     *
     * @param article article；类型为 <code>Article</code>
     */
    void insert(Article article);
    /**
     * finish固定链接标识处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_english_writing_article</code>。
     *
     * @param article article；类型为 <code>Article</code>
     */
    void finishSlug(Article article);
    /**
     * 更新写作Article。
     *
     * 持久化操作：更新，涉及 <code>sr_english_writing_article</code>。
     *
     * @param article article；类型为 <code>Article</code>
     * @return SQL 写入影响的记录行数
     */
    int update(Article article);
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_english_writing_article</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param owner owner；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("owner")</code>
     * @param version 版本；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("version")</code>
     * @param state 状态；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("state")</code>
     * @param visibility visibility；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("visibility")</code>
     * @return SQL 写入影响的记录行数
     */
    int status(@Param("id") long id,@Param("owner") long owner,@Param("version") long version,@Param("state") String state,@Param("visibility") String visibility);
    /**
     * 删除写作Article。
     *
     * 持久化操作：更新，涉及 <code>sr_english_writing_article</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param owner owner；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("owner")</code>
     * @param version 版本；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("version")</code>
     * @return SQL 写入影响的记录行数
     */
    int delete(@Param("id") long id,@Param("owner") long owner,@Param("version") long version);
}
