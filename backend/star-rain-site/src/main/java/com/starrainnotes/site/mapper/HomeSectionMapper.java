/*
 * HomeSectionMapper：站点模块的数据访问文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：HomeSectionEntity。
 * 阅读入口：all（全部处理入口）；byCode（依据代码处理入口）；update（更新首页区块）。
 */

package com.starrainnotes.site.mapper;

import com.starrainnotes.site.entity.HomeSectionEntity;

import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 首页区块数据映射，属于站点模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface HomeSectionMapper {
    /**
     * 全部处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_site_home_section</code>。
     * @return 本次处理得到的条目集合
     */
    List<HomeSectionEntity> all();

    /**
     * 依据代码处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_site_home_section</code>。
     *
     * @param code 代码；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("code")</code>
     * @return 类型为 <code>HomeSectionEntity</code> 的处理结果
     */
    HomeSectionEntity byCode(@Param("code") String code);

    /**
     * 更新首页区块。
     *
     * 持久化操作：更新，涉及 <code>sr_site_home_section</code>。
     *
     * @param section 区块；类型为 <code>HomeSectionEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int update(HomeSectionEntity section);
}
