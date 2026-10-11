/*
 * DocumentMetadataMapper：英语学习模块的数据访问文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：Node、Revision。
 * 阅读入口：tags（标签列表处理入口）；clearTags（清理标签列表）；insertTags（写入标签列表）；revisionCount（修订数量处理入口）。
 */

package com.starrainnotes.english.knowledge.mapper;
import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import com.starrainnotes.english.knowledge.dto.RevisionDto.Revision;
import java.util.List;
import org.apache.ibatis.annotations.Param;
/**
 * 文档元数据数据映射，属于英语学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface DocumentMetadataMapper {
    /**
     * 标签列表处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_taxonomy_term</code>。
     *
     * @param kind kind；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("kind")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 本次处理得到的条目集合
     */
    List<Node> tags(@Param("kind") String kind,@Param("id") long id);
    /**
     * 清理标签列表。
     *
     * 持久化语句类型：<code>delete</code>。
     *
     * @param kind kind；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("kind")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     */
    void clearTags(@Param("kind") String kind,@Param("id") long id);
    /**
     * 写入标签列表。
     *
     * 持久化语句类型：<code>insert</code>。
     *
     * @param kind kind；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("kind")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("ids")</code>
     */
    void insertTags(@Param("kind") String kind,@Param("id") long id,@Param("ids") List<Long> ids);
    /**
     * 修订数量处理入口。
     *
     * 持久化语句类型：<code>select</code>。
     *
     * @param kind kind；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("kind")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long revisionCount(@Param("kind") String kind,@Param("id") long id);
    /**
     * 下一项修订处理入口。
     *
     * 持久化语句类型：<code>select</code>。
     *
     * @param kind kind；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("kind")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
    long nextRevision(@Param("kind") String kind,@Param("id") long id);
    /**
     * 修订集合处理入口。
     *
     * 持久化语句类型：<code>select</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param kind kind；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("kind")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param offset 查询跳过的记录数；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<Revision> revisions(@Param("kind") String kind,@Param("id") long id,@Param("offset") long offset,@Param("limit") int limit);
    /**
     * 修订处理入口。
     *
     * 持久化语句类型：<code>select</code>。
     *
     * @param kind kind；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("kind")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param no 不含；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("no")</code>
     * @return 类型为 <code>Revision</code> 的处理结果
     */
    Revision revision(@Param("kind") String kind,@Param("id") long id,@Param("no") long no);
    /**
     * 写入修订。
     *
     * 持久化语句类型：<code>insert</code>。
     *
     * @param kind kind；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("kind")</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param no 不含；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("no")</code>
     * @param actor 当前操作对应的身份对象；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("actor")</code>
     * @param snapshot 快照；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("snapshot")</code>
     * @param note note；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("note")</code>
     */
    void insertRevision(@Param("kind") String kind,@Param("id") long id,@Param("no") long no,@Param("actor") long actor,@Param("snapshot") String snapshot,@Param("note") String note);
}
