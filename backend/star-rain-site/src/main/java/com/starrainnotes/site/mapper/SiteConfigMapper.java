/*
 * SiteConfigMapper：站点模块的数据访问文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：SiteConfigEntity。
 * 阅读入口：primary（primary处理入口）；update（更新站点配置）。
 */

package com.starrainnotes.site.mapper;

import com.starrainnotes.site.entity.SiteConfigEntity;

/**
 * 站点配置数据映射，属于站点模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface SiteConfigMapper {
    /**
     * primary处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_site_config</code>。
     * @return 类型为 <code>SiteConfigEntity</code> 的处理结果
     */
    SiteConfigEntity primary();

    /**
     * 更新站点配置。
     *
     * 持久化操作：更新，涉及 <code>sr_site_config</code>。
     *
     * @param config 配置；类型为 <code>SiteConfigEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int update(SiteConfigEntity config);
}
