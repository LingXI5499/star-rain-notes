/*
 * WorkMediaMapper：作品模块的数据访问文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：WorkMediaEntity。
 * 阅读入口：insert（写入作品媒体）；selectById（按标识读取查询）；listByWorkId（列出依据作品标识）；updateContent（更新内容）。
 */

package com.starrainnotes.portfolio.mapper;

import com.starrainnotes.portfolio.entity.WorkMediaEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/*
 * 作品媒体编排。SQL 全部在 mapper/portfolio/WorkMediaMapper.xml。
 *
 * 原先一次 updateById 既承担「改媒体用途与说明」，也承担「只改排序」，
 * 两条路径改动的列完全不同，因此拆成 updateContent 与 updateSortOrder，
 * 让「改排序不会顺手覆盖 caption / usage_type」成为 XML 里看得见的事实。
 */
/**
 * 作品媒体数据映射，属于作品模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface WorkMediaMapper {

    /**
     * 写入作品媒体。
     *
     * 持久化操作：新增，涉及 <code>sr_portfolio_media</code>。
     *
     * @param media 媒体；类型为 <code>WorkMediaEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(WorkMediaEntity media);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_portfolio_media</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>WorkMediaEntity</code> 的处理结果
     */
    WorkMediaEntity selectById(@Param("id") Long id);

    /**
     * 列出依据作品标识。
     *
     * 持久化操作：读取，涉及 <code>sr_portfolio_media</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("workId")</code>
     * @return 本次处理得到的条目集合
     */
    List<WorkMediaEntity> listByWorkId(@Param("workId") Long workId);

    /**
     * 更新内容。
     *
     * 持久化操作：更新，涉及 <code>sr_portfolio_media</code>。
     *
     * @param media 媒体；类型为 <code>WorkMediaEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int updateContent(WorkMediaEntity media);

    /**
     * 更新排序顺序。
     *
     * 持久化操作：更新，涉及 <code>sr_portfolio_media</code>。
     *
     * @param media 媒体；类型为 <code>WorkMediaEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int updateSortOrder(WorkMediaEntity media);

    /**
     * 删除依据标识。
     *
     * 持久化操作：删除，涉及 <code>sr_portfolio_media</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteById(@Param("id") Long id);
}
