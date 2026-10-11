/*
 * PortfolioPublicApiImpl：作品模块的跨模块接口文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：PageResult、PortfolioPublicApi、PortfolioReferenceApi、PortfolioSearchSourceApi、PortfolioPublishedWork、WorkNotFoundException、PortfolioWorkService。
 * 阅读入口：publishedWorks（已发布作品列表处理入口）；publishedWorkBySlug（按固定链接标识读取已发布作品）；exists（判断是否存在作品公开）；publishedWork（已发布作品处理入口）。
 */

package com.starrainnotes.portfolio.api.impl;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.api.PortfolioPublicApi;
import com.starrainnotes.portfolio.api.PortfolioReferenceApi;
import com.starrainnotes.portfolio.api.PortfolioSearchSourceApi;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;
import com.starrainnotes.portfolio.exception.WorkNotFoundException;
import com.starrainnotes.portfolio.service.PortfolioWorkService;
import com.starrainnotes.portfolio.vo.WorkVO;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 作品公开接口实现，属于作品模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Service
@RequiredArgsConstructor
public class PortfolioPublicApiImpl implements PortfolioPublicApi, PortfolioReferenceApi, PortfolioSearchSourceApi {
    /**
     * 作品列表。
     * 协作类型：<code>PortfolioWorkService</code>。
     */
    private final PortfolioWorkService works;
    /**
     * 内容。
     * 协作类型：<code>com.starrainnotes.portfolio.service.PortfolioContentService</code>。
     */
    private final com.starrainnotes.portfolio.service.PortfolioContentService content;

    /**
     * 已发布作品列表处理入口。
     * 主要协作调用：<code>works.publicWorks</code>、<code>works.publicWorkById</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;PortfolioPublishedWork&gt;</code> 的处理结果
     */
    @Override
    public PageResult<PortfolioPublishedWork> publishedWorks(int page, int pageSize) {
        PageResult<WorkVO> pageResult = works.publicWorks(page, pageSize, null);
        return PageResult.<PortfolioPublishedWork>builder()
                .items(pageResult.getItems().stream().map(item -> published(works.publicWorkById(Long.valueOf(item.getId())))).toList())
                .page(pageResult.getPage()).pageSize(pageResult.getPageSize())
                .total(pageResult.getTotal()).build();
    }

    /**
     * 按固定链接标识读取已发布作品。
     * 主要协作调用：<code>works.publicWork</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    public Optional<PortfolioPublishedWork> publishedWorkBySlug(String slug) {
        try {
            return Optional.of(published(works.publicWork(slug)));
        } catch (WorkNotFoundException exception) {
            return Optional.empty();
        }
    }

    /**
     * 判断是否存在作品公开。
     * 主要协作调用：<code>works.adminWork</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean exists(Long workId) {
        try {
            works.adminWork(workId);
            return true;
        } catch (WorkNotFoundException exception) {
            return false;
        }
    }

    /**
     * 已发布作品处理入口。
     * 主要协作调用：<code>works.publicWorkById</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    public Optional<PortfolioPublishedWork> publishedWork(Long workId) {
        try {
            return Optional.of(published(works.publicWorkById(workId)));
        } catch (WorkNotFoundException exception) {
            return Optional.empty();
        }
    }

    /**
     * 来源分页处理入口。
     * 关键处理步骤调用 <code>publishedWorks</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;PortfolioPublishedWork&gt;</code> 的处理结果
     */
    @Override
    public PageResult<PortfolioPublishedWork> sourcePage(int page, int pageSize) {
        return publishedWorks(page, pageSize);
    }

    /**
     * 已发布处理入口。
     * 主要协作调用：<code>content.seoMarkdown</code>。
     *
     * @param work 作品；类型为 <code>WorkVO</code>
     * @return 类型为 <code>PortfolioPublishedWork</code> 的处理结果
     */
    private PortfolioPublishedWork published(WorkVO work) {
        return PortfolioPublishedWork.builder().id(Long.valueOf(work.getId()))
                .slug(work.getSlug()).workType(work.getWorkType()).title(work.getTitle())
                .summary(work.getSummary()).coverUrl(work.getCoverUrl())
                .bodyMarkdown(content.seoMarkdown(Long.valueOf(work.getId()), work.getBodyMarkdown()))
                .searchableText(work.getSearchableText()).seoTitle(work.getSeoTitle()).seoDescription(work.getSeoDescription())
                .publishedAt(work.getPublishedAt()).updatedAt(work.getUpdatedAt()).build();
    }
}
