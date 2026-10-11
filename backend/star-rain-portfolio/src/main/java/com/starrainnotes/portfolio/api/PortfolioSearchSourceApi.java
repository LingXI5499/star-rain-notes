/*
 * PortfolioSearchSourceApi：作品模块的跨模块接口文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：PageResult、PortfolioPublishedWork。
 * 阅读入口：sourcePage（来源分页处理入口）。
 */

package com.starrainnotes.portfolio.api;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;

/**
 * 作品搜索来源接口，属于作品模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface PortfolioSearchSourceApi {
    /**
     * 来源分页处理入口。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;PortfolioPublishedWork&gt;</code> 的处理结果
     */
    PageResult<PortfolioPublishedWork> sourcePage(int page, int pageSize);
}
