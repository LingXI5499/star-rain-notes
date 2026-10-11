/*
 * PortfolioReferenceApi：作品模块的跨模块接口文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：PortfolioPublishedWork。
 * 阅读入口：exists（判断是否存在作品引用）；publishedWork（已发布作品处理入口）。
 */

package com.starrainnotes.portfolio.api;

import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;
import java.util.Optional;

/**
 * 作品引用接口，属于作品模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface PortfolioReferenceApi {
    /**
     * 判断是否存在作品引用。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    boolean exists(Long workId);
    /**
     * 已发布作品处理入口。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    Optional<PortfolioPublishedWork> publishedWork(Long workId);
}
