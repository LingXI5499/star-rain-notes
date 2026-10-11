/*
 * SearchDocumentMapper：搜索模块的数据访问文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：SearchableDocument、SearchHitVO、SearchSuggestionVO。
 * 阅读入口：upsert（新增或更新搜索文档）；remove（移除搜索文档）；removeByContent（移除依据内容）；removeRoutePrefix（移除路由Prefix）。
 */

package com.starrainnotes.search.mapper;

import com.starrainnotes.search.api.dto.SearchableDocument;
import com.starrainnotes.search.vo.SearchHitVO;
import com.starrainnotes.search.vo.SearchSuggestionVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 搜索文档数据映射，属于搜索模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface SearchDocumentMapper {
    /**
     * 新增或更新搜索文档。
     *
     * 持久化操作：新增，涉及 <code>sr_search_document</code>。
     *
     * @param document 文档；类型为 <code>SearchableDocument</code>
     * @return SQL 写入影响的记录行数
     */
    int upsert(SearchableDocument document);
    /**
     * 移除搜索文档。
     *
     * 持久化操作：更新，涉及 <code>sr_search_document</code>。
     *
     * @param documentKey 文档键；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("documentKey")</code>
     * @return SQL 写入影响的记录行数
     */
    int remove(@Param("documentKey") String documentKey);
    /**
     * 移除依据内容。
     *
     * 持久化操作：更新，涉及 <code>sr_search_document</code>。
     *
     * @param contentType 内容类型或请求声明的 Content-Type；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("contentType")</code>
     * @param contentId 内容标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("contentId")</code>
     * @return SQL 写入影响的记录行数
     */
    int removeByContent(@Param("contentType") String contentType, @Param("contentId") Long contentId);
    /**
     * 移除路由Prefix。
     *
     * 持久化操作：更新，涉及 <code>sr_search_document</code>。
     *
     * @param prefix prefix；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("prefix")</code>
     * @return SQL 写入影响的记录行数
     */
    int removeRoutePrefix(@Param("prefix") String prefix);
    /**
     * 有效Keys处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_search_document</code>。
     *
     * @param contentType 内容类型或请求声明的 Content-Type；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("contentType")</code>
     * @return 本次处理得到的条目集合
     */
    List<String> activeKeys(@Param("contentType") String contentType);
    /**
     * 有效数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_search_document</code>。
     * @return 符合 SQL 条件的记录数量
     */
    long activeCount();
    /**
     * 统计搜索文档。
     *
     * 持久化操作：读取，涉及 <code>sr_search_document</code>。
     *
     * @param query 查询参数；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("query")</code>
     * @param types types；类型为 <code>List&lt;String&gt;</code>；对应 MyBatis 命名参数 <code>@Param("types")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long count(@Param("query") String query, @Param("types") List<String> types);
    /**
     * 搜索处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_search_document</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param query 查询参数；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("query")</code>
     * @param types types；类型为 <code>List&lt;String&gt;</code>；对应 MyBatis 命名参数 <code>@Param("types")</code>
     * @param offset 查询跳过的记录数；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<SearchHitVO> search(@Param("query") String query, @Param("types") List<String> types,
                             @Param("offset") long offset, @Param("limit") int limit);
    /**
     * suggestions处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_search_document</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param query 查询参数；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("query")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<SearchSuggestionVO> suggestions(@Param("query") String query, @Param("limit") int limit);
}
