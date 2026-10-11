/*
 * WorkDetailMapper：作品模块的数据访问文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：WorkDetailEntity。
 * 阅读入口：insert（写入作品详情）；selectByWorkId（按作品标识读取查询）；updateDetailJson（更新详情JSON）；deleteById（删除依据标识）。
 */

package com.starrainnotes.portfolio.mapper;

import com.starrainnotes.portfolio.entity.WorkDetailEntity;
import org.apache.ibatis.annotations.Param;

/*
 * 作品类型扩展信息（每个作品最多一行）。SQL 全部在 mapper/portfolio/WorkDetailMapper.xml。
 *
 * 这张表只有「按 work_id 读一行」「新增」「改 detail_json」「删除」四个访问形状，
 * 逐个显式声明比让 BaseMapper 生成整套 CRUD 更能说明它只按作品维度使用。
 */
/**
 * 作品详情数据映射，属于作品模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface WorkDetailMapper {

    /**
     * 写入作品详情。
     *
     * 持久化操作：新增，涉及 <code>sr_portfolio_work_detail</code>。
     *
     * @param detail 详情；类型为 <code>WorkDetailEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(WorkDetailEntity detail);

    /**
     * 按作品标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_portfolio_work_detail</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("workId")</code>
     * @return 类型为 <code>WorkDetailEntity</code> 的处理结果
     */
    WorkDetailEntity selectByWorkId(@Param("workId") Long workId);

    /**
     * 更新详情JSON。
     *
     * 持久化操作：更新，涉及 <code>sr_portfolio_work_detail</code>。
     *
     * @param detail 详情；类型为 <code>WorkDetailEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int updateDetailJson(WorkDetailEntity detail);

    /**
     * 删除依据标识。
     *
     * 持久化操作：删除，涉及 <code>sr_portfolio_work_detail</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteById(@Param("id") Long id);
}
