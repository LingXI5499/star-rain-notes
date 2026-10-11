/*
 * PortfolioPublicApi：作品模块的跨模块接口文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：PageResult、PortfolioPublishedWork。
 * 阅读入口：publishedWorks（已发布作品列表处理入口）；publishedWorkBySlug（按固定链接标识读取已发布作品）。
 */

package com.starrainnotes.portfolio.api;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;
import java.util.Optional;

/**
 * 作品公开接口，属于作品模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface PortfolioPublicApi {
    /**
     * 已发布作品列表处理入口。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;PortfolioPublishedWork&gt;</code> 的处理结果
     */
    PageResult<PortfolioPublishedWork> publishedWorks(int page, int pageSize);
    /**
     * 按固定链接标识读取已发布作品。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    Optional<PortfolioPublishedWork> publishedWorkBySlug(String slug);
}
