/*
 * TaxonomyMapper：英语学习模块的数据访问文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：Node。
 * 阅读入口：list（列出Taxonomy）；byId（依据标识处理入口）；bySlug（依据固定链接标识处理入口）；insert（写入Taxonomy）。
 */

package com.starrainnotes.english.taxonomy.mapper;
import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import java.util.List;
import org.apache.ibatis.annotations.Param;
/**
 * Taxonomy数据映射，属于英语学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface TaxonomyMapper {
    /**
     * 列出Taxonomy。
     *
     * 持久化操作：读取，涉及 <code>sr_english_taxonomy_term</code>。
     *
     * @param admin 后台；类型为 <code>boolean</code>；对应 MyBatis 命名参数 <code>@Param("admin")</code>
     * @return 本次处理得到的条目集合
     */
    List<Node> list(@Param("admin") boolean admin);
    /**
     * 依据标识处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_taxonomy_term</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>Node</code> 的处理结果
     */
    Node byId(@Param("id") long id);
    /**
     * 依据固定链接标识处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_taxonomy_term</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @return 类型为 <code>Node</code> 的处理结果
     */
    Node bySlug(@Param("slug") String slug);
    /**
     * 写入Taxonomy。
     *
     * 持久化操作：新增，涉及 <code>sr_english_taxonomy_term</code>。
     *
     * @param node node；类型为 <code>Node</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(Node node);
    /**
     * 更新Taxonomy。
     *
     * 持久化操作：更新，涉及 <code>sr_english_taxonomy_term</code>。
     *
     * @param node node；类型为 <code>Node</code>
     * @return SQL 写入影响的记录行数
     */
    int update(Node node);
    /**
     * 禁用Taxonomy。
     *
     * 持久化操作：更新，涉及 <code>sr_english_taxonomy_term</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int disable(@Param("id") long id);
}
