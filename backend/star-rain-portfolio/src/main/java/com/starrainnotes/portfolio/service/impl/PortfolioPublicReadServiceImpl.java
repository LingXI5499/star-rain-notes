/*
 * PortfolioPublicReadServiceImpl：作品模块的业务实现文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：AnalyticsRecordApi、ContentViewEvent、PortfolioPublicReadService、PortfolioWorkService、WorkVO。
 * 阅读入口：read（读取作品公开读取）。
 */

package com.starrainnotes.portfolio.service.impl;

import com.starrainnotes.analytics.api.AnalyticsRecordApi;
import com.starrainnotes.analytics.api.ContentViewEvent;
import com.starrainnotes.portfolio.service.PortfolioPublicReadService;
import com.starrainnotes.portfolio.service.PortfolioWorkService;
import com.starrainnotes.portfolio.vo.WorkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 作品公开读取业务服务实现，属于作品模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class PortfolioPublicReadServiceImpl implements PortfolioPublicReadService {
    /**
     * 作品列表。
     * 协作类型：<code>PortfolioWorkService</code>。
     */
    private final PortfolioWorkService works;
    /**
     * 统计。
     */
    private final ObjectProvider<AnalyticsRecordApi> analytics;

    /**
     * 读取作品公开读取。
     * 主要协作调用：<code>works.publicWork</code>、<code>analytics.getIfAvailable</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    public WorkVO read(String slug) {
        WorkVO detail = works.publicWork(slug);
        AnalyticsRecordApi recorder = analytics.getIfAvailable();
        if (recorder != null)
            recorder.recordContentView(new ContentViewEvent(
                    "PORTFOLIO", Long.valueOf(detail.getId()), "/portfolio/:slug"));
        return detail;
    }
}
