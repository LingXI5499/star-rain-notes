/*
 * WorkLinkMapper：作品模块的数据访问文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：WorkLinkEntity。
 * 阅读入口：insert（写入作品链接）；selectById（按标识读取查询）；listByWorkId（列出依据作品标识）；updateContent（更新内容）。
 */

package com.starrainnotes.portfolio.mapper;

import com.starrainnotes.portfolio.entity.WorkLinkEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/*
 * 作品外部链接。SQL 全部在 mapper/portfolio/WorkLinkMapper.xml。
 *
 * 与媒体一样，原先一次 updateById 同时承担「改链接内容」和「只改排序」，
 * 迁移后拆成两条列范围明确的 UPDATE。
 */
/**
 * 作品链接数据映射，属于作品模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface WorkLinkMapper {

    /**
     * 写入作品链接。
     *
     * 持久化操作：新增，涉及 <code>sr_portfolio_link</code>。
     *
     * @param link 链接；类型为 <code>WorkLinkEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(WorkLinkEntity link);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_portfolio_link</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>WorkLinkEntity</code> 的处理结果
     */
    WorkLinkEntity selectById(@Param("id") Long id);

    /**
     * 列出依据作品标识。
     *
     * 持久化操作：读取，涉及 <code>sr_portfolio_link</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("workId")</code>
     * @return 本次处理得到的条目集合
     */
    List<WorkLinkEntity> listByWorkId(@Param("workId") Long workId);

    /**
     * 更新内容。
     *
     * 持久化操作：更新，涉及 <code>sr_portfolio_link</code>。
     *
     * @param link 链接；类型为 <code>WorkLinkEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int updateContent(WorkLinkEntity link);

    /**
     * 更新排序顺序。
     *
     * 持久化操作：更新，涉及 <code>sr_portfolio_link</code>。
     *
     * @param link 链接；类型为 <code>WorkLinkEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int updateSortOrder(WorkLinkEntity link);

    /**
     * 删除依据标识。
     *
     * 持久化操作：删除，涉及 <code>sr_portfolio_link</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteById(@Param("id") Long id);
}
