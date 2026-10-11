/*
 * WorkMapper：作品模块的数据访问文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：WorkEntity。
 * 阅读入口：insert（写入作品）；selectById（按标识读取查询）；byIdForUpdate（依据标识面向更新处理入口）；selectBySlugAndStatus（按固定链接标识及状态读取查询）。
 */

package com.starrainnotes.portfolio.mapper;

import com.starrainnotes.portfolio.entity.WorkEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/*
 * 作品主表。全部 SQL 在 mapper/portfolio/WorkMapper.xml。
 *
 * 不再继承 MyBatis-Plus 的 BaseMapper：selectById / selectList / selectCount / updateById
 * 这些语句由框架运行期拼装，源码里看不到实际 SQL，既不符合「所有实际 SQL 写在 XML」的约定，
 * 也让人无法核对「一次 updateById 到底改了哪几列」。这里按访问形状逐个显式声明。
 *
 * updateById 在原先的写法里有两种不同的后果，迁移时分别用独立方法表达：
 *   - 服务层先改字段再保存（改标题、改正文、发布、撤回、恢复、只touch时间戳）；
 *   - 保存的是刚从库里读出来的整行，等于把读出来的值原样写回，实际不改动它。
 * 因此下面每个 UPDATE 都只列该路径真正会写的那几列。
 */
/**
 * 作品数据映射，属于作品模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface WorkMapper {

    /**
     * 写入作品。
     *
     * 持久化操作：新增，涉及 <code>sr_portfolio_work</code>。
     *
     * @param work 作品；类型为 <code>WorkEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(WorkEntity work);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_portfolio_work</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>WorkEntity</code> 的处理结果
     */
    WorkEntity selectById(@Param("id") Long id);

    /**
     * 依据标识面向更新处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_portfolio_work</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>WorkEntity</code> 的处理结果
     */
    WorkEntity byIdForUpdate(@Param("id") Long id);

    /**
     * 按固定链接标识及状态读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_portfolio_work</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @return 类型为 <code>WorkEntity</code> 的处理结果
     */
    WorkEntity selectBySlugAndStatus(@Param("slug") String slug, @Param("status") String status);

    /**
     * 按标识及状态读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_portfolio_work</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @return 类型为 <code>WorkEntity</code> 的处理结果
     */
    WorkEntity selectByIdAndStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 统计依据筛选。
     *
     * 持久化操作：读取，涉及 <code>sr_portfolio_work</code>。
     *
     * @param workType 作品类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("workType")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countByFilter(@Param("workType") String workType, @Param("status") String status,
                       @Param("keyword") String keyword);

    /**
     * 分页读取依据筛选。
     *
     * 持久化操作：读取，涉及 <code>sr_portfolio_work</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param workType 作品类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("workType")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @param offset 查询跳过的记录数；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @return 本次处理得到的条目集合
     */
    List<WorkEntity> pageByFilter(@Param("workType") String workType, @Param("status") String status,
                                  @Param("keyword") String keyword,
                                  @Param("limit") int limit, @Param("offset") long offset);

    /**
     * 统计依据Taxonomy。
     *
     * 持久化操作：读取，涉及 <code>sr_portfolio_work</code>。
     *
     * @param workType 作品类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("workType")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @param filter 筛选；类型为 <code>com.starrainnotes.portfolio.dto.WorkFilterDTO</code>；对应 MyBatis 命名参数 <code>@Param("filter")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countByTaxonomy(@Param("workType") String workType, @Param("status") String status,
        @Param("keyword") String keyword, @Param("filter") com.starrainnotes.portfolio.dto.WorkFilterDTO filter);
    /**
     * 分页读取依据Taxonomy。
     *
     * 持久化操作：读取，涉及 <code>sr_portfolio_work</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param workType 作品类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("workType")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @param filter 筛选；类型为 <code>com.starrainnotes.portfolio.dto.WorkFilterDTO</code>；对应 MyBatis 命名参数 <code>@Param("filter")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @param offset 查询跳过的记录数；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @return 本次处理得到的条目集合
     */
    List<WorkEntity> pageByTaxonomy(@Param("workType") String workType, @Param("status") String status,
        @Param("keyword") String keyword, @Param("filter") com.starrainnotes.portfolio.dto.WorkFilterDTO filter,
        @Param("limit") int limit, @Param("offset") long offset);
    /**
     * 统计依据固定链接标识。
     *
     * 持久化操作：读取，涉及 <code>sr_portfolio_work</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countBySlug(@Param("slug") String slug);

    /* 编辑工作区基础信息：只写 slug / title / summary 与审计列 */
    /**
     * 更新内容。
     *
     * 持久化操作：更新，涉及 <code>sr_portfolio_work</code>。
     *
     * @param work 作品；类型为 <code>WorkEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int updateContent(WorkEntity work);
    /**
     * 更新Prototype。
     *
     * 持久化操作：更新，涉及 <code>sr_portfolio_work</code>。
     *
     * @param work 作品；类型为 <code>WorkEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int updatePrototype(WorkEntity work);
    /**
     * 上一项已发布处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_portfolio_work</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param sortOrder 展示排序值；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("sortOrder")</code>
     * @return 类型为 <code>com.starrainnotes.portfolio.vo.WorkNavigationVO</code> 的处理结果
     */
    com.starrainnotes.portfolio.vo.WorkNavigationVO previousPublished(@Param("id") Long id, @Param("sortOrder") int sortOrder);
    /**
     * 下一项已发布处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_portfolio_work</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param sortOrder 展示排序值；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("sortOrder")</code>
     * @return 类型为 <code>com.starrainnotes.portfolio.vo.WorkNavigationVO</code> 的处理结果
     */
    com.starrainnotes.portfolio.vo.WorkNavigationVO nextPublished(@Param("id") Long id, @Param("sortOrder") int sortOrder);

    /* 正文单独保存：正文可能有 1MB，不能跟着基础信息一起整行写 */
    /**
     * 更新正文。
     *
     * 持久化操作：更新，涉及 <code>sr_portfolio_work</code>。
     *
     * @param work 作品；类型为 <code>WorkEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int updateBody(WorkEntity work);

    /* 媒体、外链、类型详情的增删改只更新作品的审计列 */
    /**
     * touch处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_portfolio_work</code>。
     *
     * @param work 作品；类型为 <code>WorkEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int touch(WorkEntity work);

    /**
     * 发布作品。
     *
     * 持久化操作：更新，涉及 <code>sr_portfolio_work</code>。
     *
     * @param work 作品；类型为 <code>WorkEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int publish(WorkEntity work);

    /**
     * 撤回作品。
     *
     * 持久化操作：更新，涉及 <code>sr_portfolio_work</code>。
     *
     * @param work 作品；类型为 <code>WorkEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int withdraw(WorkEntity work);

    /**
     * 恢复作品。
     *
     * 持久化操作：更新，涉及 <code>sr_portfolio_work</code>。
     *
     * @param work 作品；类型为 <code>WorkEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int restore(WorkEntity work);

    /**
     * 删除依据标识。
     *
     * 持久化操作：删除，涉及 <code>sr_portfolio_work</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteById(@Param("id") Long id);
}
