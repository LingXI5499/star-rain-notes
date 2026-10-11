/*
 * SeoPageMapper：公开页面与 SEO模块的数据访问文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：SeoPageSnapshot。
 * 阅读入口：upsert（新增或更新SEO分页）；remove（移除SEO分页）；active（有效处理入口）；activeMeta（有效Meta处理入口）。
 */

package com.starrainnotes.seo.mapper;

import com.starrainnotes.seo.api.dto.SeoPageSnapshot;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SEO分页数据映射，属于公开页面与 SEO模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface SeoPageMapper {
    /**
     * 新增或更新SEO分页。
     *
     * 持久化操作：新增，涉及 <code>sr_seo_page_snapshot</code>。
     *
     * @param snapshot 快照；类型为 <code>SeoPageSnapshot</code>
     * @return SQL 写入影响的记录行数
     */
    int upsert(SeoPageSnapshot snapshot);
    /**
     * 移除SEO分页。
     *
     * 持久化操作：更新，涉及 <code>sr_seo_page_snapshot</code>。
     *
     * @param routePath 路由路径；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("routePath")</code>
     * @return SQL 写入影响的记录行数
     */
    int remove(@Param("routePath") String routePath);
    /**
     * 有效处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_seo_page_snapshot</code>。
     *
     * @param routePath 路由路径；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("routePath")</code>
     * @return 类型为 <code>SeoPageSnapshot</code> 的处理结果
     */
    SeoPageSnapshot active(@Param("routePath") String routePath);
    /**
     * 有效Meta处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_seo_page_snapshot</code>。
     *
     * @param routePath 路由路径；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("routePath")</code>
     * @return 类型为 <code>SeoPageSnapshot</code> 的处理结果
     */
    SeoPageSnapshot activeMeta(@Param("routePath") String routePath);
    /**
     * 有效Pages处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_seo_page_snapshot</code>。
     * @return 本次处理得到的条目集合
     */
    List<SeoPageSnapshot> activePages();
    /**
     * 有效Paths处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_seo_page_snapshot</code>。
     * @return 本次处理得到的条目集合
     */
    List<String> activePaths();
    /**
     * 按Prefix读取有效Paths。
     *
     * 持久化操作：读取，涉及 <code>sr_seo_page_snapshot</code>。
     *
     * @param prefix prefix；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("prefix")</code>
     * @return 本次处理得到的条目集合
     */
    List<String> activePathsByPrefix(@Param("prefix") String prefix);
    /**
     * 有效数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_seo_page_snapshot</code>。
     * @return 符合 SQL 条件的记录数量
     */
    long activeCount();
}
