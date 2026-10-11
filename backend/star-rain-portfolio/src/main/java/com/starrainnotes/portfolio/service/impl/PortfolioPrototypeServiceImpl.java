/*
 * PortfolioPrototypeServiceImpl：作品模块的业务实现文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：CurrentActorApi、StaticPrototypeApi、MediaReferenceApi、MediaReferenceCommand、WorkPrototypeDTO、WorkEntity、WorkInvalidException。
 * 阅读入口：bind（绑定作品Prototype）；read（读取作品Prototype）；contentType（内容类型或请求声明的 Content-Type处理入口）。
 */

package com.starrainnotes.portfolio.service.impl;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.media.api.StaticPrototypeApi;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.portfolio.dto.WorkPrototypeDTO;
import com.starrainnotes.portfolio.entity.WorkEntity;
import com.starrainnotes.portfolio.exception.WorkInvalidException;
import com.starrainnotes.portfolio.exception.WorkNotFoundException;
import com.starrainnotes.portfolio.mapper.WorkMapper;
import com.starrainnotes.portfolio.event.WorkEventPublisher;
import com.starrainnotes.portfolio.api.event.WorkPublicationChangedEvent;
import com.starrainnotes.portfolio.service.PortfolioPrototypeService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 作品Prototype业务服务实现，属于作品模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class PortfolioPrototypeServiceImpl implements PortfolioPrototypeService {
    /**
     * 作品列表。
     * 协作类型：<code>WorkMapper</code>。
     */
    private final WorkMapper works;
    /**
     * prototypes。
     * 协作类型：<code>StaticPrototypeApi</code>。
     */
    private final StaticPrototypeApi prototypes;
    /**
     * references。
     * 协作类型：<code>MediaReferenceApi</code>。
     */
    private final MediaReferenceApi references;
    /**
     * 当前操作对应的身份对象。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi actor;
    /**
     * 事件集合。
     */
    private final WorkEventPublisher events;
    /**
     * 绑定作品Prototype。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>works.byIdForUpdate</code>、<code>prototypes.validate</code>、<code>references.detachAll</code>、<code>references.attach</code>、<code>actor.current</code>。
     * 实现中的显式失败类型包括 <code>WorkNotFoundException</code>、<code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkPrototypeDTO</code>
     */
    @Override @Transactional public void bind(Long workId, WorkPrototypeDTO request) {
        WorkEntity work = works.byIdForUpdate(workId);
        if (work == null) { throw new WorkNotFoundException(); }
        if (request.getMediaAssetId() != null) {
            if (!"SOFTWARE".equals(work.getWorkType())) { throw new WorkInvalidException("静态原型用于软件作品"); }
            prototypes.validate(request.getMediaAssetId(), request.getEntryPath());
        }
        references.detachAll("PORTFOLIO", "WORK_PROTOTYPE", workId);
        if (request.getMediaAssetId() != null) {
            references.attach(MediaReferenceCommand.builder().mediaAssetId(request.getMediaAssetId())
                    .sourceModule("PORTFOLIO").sourceType("WORK_PROTOTYPE").sourceId(workId).usageCode(com.starrainnotes.media.api.constant.MediaUsageCodes.PORTFOLIO_PROTOTYPE).build());
        }
        work.setPrototypeAssetId(request.getMediaAssetId());
        work.setPrototypeEntry(request.getMediaAssetId() == null ? null : request.getEntryPath());
        work.setUpdatedByAccountId(actor.current().getAccountId());
        work.setUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        works.updatePrototype(work);
        if ("PUBLISHED".equals(work.getStatus())) { events.afterCommit(WorkPublicationChangedEvent.builder().workId(workId).slug(work.getSlug()).published(true).build()); }
    }
    /**
     * 读取作品Prototype。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>works.selectBySlugAndStatus</code>、<code>prototypes.read</code>。
     * 实现中的显式失败类型包括 <code>WorkNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param path 路径；类型为 <code>String</code>
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    @Override @Transactional(readOnly = true) public byte[] read(String slug, String path) {
        WorkEntity work = works.selectBySlugAndStatus(slug, "PUBLISHED");
        if (work == null || work.getPrototypeAssetId() == null) { throw new WorkNotFoundException(); }
        return prototypes.read(work.getPrototypeAssetId(), path);
    }
    /**
     * 内容类型或请求声明的 Content-Type处理入口。
     * 主要协作调用：<code>prototypes.contentType</code>。
     *
     * @param path 路径；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override public String contentType(String path) { return prototypes.contentType(path); }
}
