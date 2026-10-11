/*
 * PortfolioWorkServiceImpl：作品模块的业务实现文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：CurrentActorApi、PageResult、MediaAssetApi、MediaReferenceApi、MediaAssetSummary、MediaReferenceCommand、IdOrderDTO。
 * 阅读入口：publicWorks（公开作品列表处理入口）；publicWork（公开作品处理入口）；publicWorkById（按标识读取公开作品）；adminWorks（后台作品列表处理入口）。
 */

package com.starrainnotes.portfolio.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.media.api.MediaAssetApi;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaAssetSummary;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.portfolio.dto.IdOrderDTO;
import com.starrainnotes.portfolio.dto.WorkCreateDTO;
import com.starrainnotes.portfolio.dto.WorkLinkDTO;
import com.starrainnotes.portfolio.dto.WorkMediaDTO;
import com.starrainnotes.portfolio.dto.WorkPatchDTO;
import com.starrainnotes.portfolio.entity.WorkDetailEntity;
import com.starrainnotes.portfolio.entity.WorkEntity;
import com.starrainnotes.portfolio.entity.WorkLinkEntity;
import com.starrainnotes.portfolio.entity.WorkMediaEntity;
import com.starrainnotes.portfolio.enumeration.WorkMediaUsage;
import com.starrainnotes.portfolio.enumeration.WorkStatus;
import com.starrainnotes.portfolio.enumeration.WorkType;
import com.starrainnotes.portfolio.event.WorkEventPublisher;
import com.starrainnotes.portfolio.api.event.WorkPublicationChangedEvent;
import com.starrainnotes.portfolio.exception.WorkInvalidException;
import com.starrainnotes.portfolio.exception.WorkLinkNotFoundException;
import com.starrainnotes.portfolio.exception.WorkMediaConflictException;
import com.starrainnotes.portfolio.exception.WorkMediaNotFoundException;
import com.starrainnotes.portfolio.exception.WorkNotFoundException;
import com.starrainnotes.portfolio.exception.WorkSlugConflictException;
import com.starrainnotes.portfolio.exception.WorkStateInvalidException;
import com.starrainnotes.portfolio.exception.WorkTypeImmutableException;
import com.starrainnotes.portfolio.mapper.WorkDetailMapper;
import com.starrainnotes.portfolio.mapper.WorkLinkMapper;
import com.starrainnotes.portfolio.mapper.WorkMapper;
import com.starrainnotes.portfolio.mapper.WorkMediaMapper;
import com.starrainnotes.portfolio.service.PortfolioWorkService;
import com.starrainnotes.portfolio.utils.WorkLinkRules;
import com.starrainnotes.portfolio.utils.WorkSlugRules;
import com.starrainnotes.portfolio.validator.WorkDetailValidator;
import com.starrainnotes.portfolio.vo.WorkLinkVO;
import com.starrainnotes.portfolio.vo.WorkMediaVO;
import com.starrainnotes.portfolio.vo.WorkVO;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 作品作品业务服务实现，属于作品模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class PortfolioWorkServiceImpl implements PortfolioWorkService {
    /**
     * MODULE。
     */
    private static final String SOURCE_MODULE = "PORTFOLIO";
    /**
     * 类型。
     */
    private static final String SOURCE_TYPE = "WORK_MEDIA";
    /**
     * 内容。
     * 协作类型：<code>com.starrainnotes.portfolio.service.PortfolioContentService</code>。
     */
    private final com.starrainnotes.portfolio.service.PortfolioContentService content;
    /**
     * 作品列表。
     * 协作类型：<code>WorkMapper</code>。
     */
    private final WorkMapper works;
    /**
     * 详情。
     * 协作类型：<code>WorkDetailMapper</code>。
     */
    private final WorkDetailMapper details;
    /**
     * 媒体。
     * 协作类型：<code>WorkMediaMapper</code>。
     */
    private final WorkMediaMapper media;
    /**
     * links。
     * 协作类型：<code>WorkLinkMapper</code>。
     */
    private final WorkLinkMapper links;
    /**
     * 媒体Assets。
     * 协作类型：<code>MediaAssetApi</code>。
     */
    private final MediaAssetApi mediaAssets;
    /**
     * 媒体References。
     * 协作类型：<code>MediaReferenceApi</code>。
     */
    private final MediaReferenceApi mediaReferences;
    /**
     * 当前操作对应的身份对象。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi actor;
    /**
     * 项目配置的 Jackson JSON 读写器。
     * 协作类型：<code>ObjectMapper</code>。
     */
    private final ObjectMapper objectMapper;
    /**
     * validators。
     */
    private final List<WorkDetailValidator> validators;
    /**
     * 事件集合。
     */
    private final WorkEventPublisher events;

    /**
     * 公开作品列表处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>list</code>、<code>WorkStatus.PUBLISHED.name</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param type 类型；类型为 <code>WorkType</code>
     * @return 类型为 <code>PageResult&lt;WorkVO&gt;</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkVO> publicWorks(int page, int pageSize, WorkType type) {
        return list(page, pageSize, type, WorkStatus.PUBLISHED.name(), null, true);
    }

    /**
     * 公开作品处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>works.selectBySlugAndStatus</code>。
     * 实现中的显式失败类型包括 <code>WorkNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public WorkVO publicWork(String slug) {
        WorkEntity work = works.selectBySlugAndStatus(slug, WorkStatus.PUBLISHED.name());
        if (work == null) {
            throw new WorkNotFoundException();
        }
        return view(work, true, true);
    }

    /**
     * 按标识读取公开作品。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>works.selectByIdAndStatus</code>。
     * 实现中的显式失败类型包括 <code>WorkNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public WorkVO publicWorkById(Long id) {
        WorkEntity work = id == null ? null : works.selectByIdAndStatus(id, WorkStatus.PUBLISHED.name());
        if (work == null) {
            throw new WorkNotFoundException();
        }
        return view(work, true, true);
    }

    /**
     * 后台作品列表处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>status.isBlank</code>、<code>WorkStatus.valueOf</code>、<code>list</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param type 类型；类型为 <code>WorkType</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @param keyword 关键词；类型为 <code>String</code>
     * @return 类型为 <code>PageResult&lt;WorkVO&gt;</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkVO> adminWorks(int page, int pageSize, WorkType type,
                                         String status, String keyword) {
        if (status != null && !status.isBlank()) {
            try {
                WorkStatus.valueOf(status);
            } catch (IllegalArgumentException exception) {
                throw new WorkInvalidException("作品状态无效");
            }
        }
        return list(page, pageSize, type, status, keyword, false);
    }

    /**
     * 后台作品处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>view</code>、<code>require</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public WorkVO adminWork(Long id) {
        return view(require(id), true, false);
    }

    /**
     * 创建作品作品。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>objectMapper.valueToTree</code>、<code>actor.current</code>、<code>works.insert</code>、<code>content.setTags</code>、<code>content.copyTemplate</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>、<code>WorkSlugConflictException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>WorkCreateDTO</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    @Transactional
    public WorkVO create(WorkCreateDTO request) {
        String title = requiredText(request.getTitle(), 255, "标题");
        WorkEntity work = new WorkEntity();
        work.setSlug(uniqueGeneratedSlug(title));
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            if (!WorkSlugRules.valid(request.getSlug().trim())) { throw new WorkInvalidException("作品地址格式无效"); }
            work.setSlug(request.getSlug().trim());
        }
        work.setWorkType(request.getWorkType().name());
        work.setTitle(title);
        work.setSummary(optionalText(request.getSummary(), 1000));
        work.setBodyMarkdown("");
        work.setStatus(WorkStatus.DRAFT.name());
        work.setProjectStatus("COMPLETED"); work.setFeatured(false); work.setSortOrder(0);
        metadata(work, objectMapper.valueToTree(request));
        work.setCreatedByAccountId(actor.current().getAccountId());
        work.setUpdatedByAccountId(actor.current().getAccountId());
        try {
            works.insert(work);
        } catch (DuplicateKeyException exception) {
            throw new WorkSlugConflictException();
        }
        content.setTags(work.getId(), request.getTagIds());
        content.copyTemplate(work.getId(), request.getTemplateId());
        return view(work, true, false);
    }

    /**
     * 更新作品作品。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>objectMapper.valueToTree</code>、<code>content.setTags</code>、<code>events.afterCommit</code>、<code>works.updateContent</code>。
     * 实现中的显式失败类型包括 <code>WorkTypeImmutableException</code>、<code>WorkInvalidException</code>、<code>WorkSlugConflictException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkPatchDTO</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    @Transactional
    public WorkVO update(Long id, WorkPatchDTO request) {
        WorkEntity work = lock(id);
        String previousSlug = work.getSlug();
        if (request.getWorkType() != null) {
            throw new WorkTypeImmutableException();
        }
        if (request.getTitle() != null) {
            work.setTitle(requiredText(request.getTitle(), 255, "标题"));
        }
        if (request.getSummary() != null) {
            work.setSummary(optionalText(request.getSummary(), 1000));
        }
        if (request.getSlug() != null) {
            String slug = request.getSlug().trim();
            if (!WorkSlugRules.valid(slug)) {
                throw new WorkInvalidException("作品地址格式无效");
            }
            work.setSlug(slug);
        }
        metadata(work, objectMapper.valueToTree(request));
        content.setTags(id, request.getTagIds());
        if ("PUBLISHED".equals(work.getStatus()) && !previousSlug.equals(work.getSlug())) {
            events.afterCommit(new WorkPublicationChangedEvent(work.getId(), previousSlug, false));
        }
        touch(work);
        if ("PUBLISHED".equals(work.getStatus())) { validatePublish(work); }
        try {
            works.updateContent(work);
        } catch (DuplicateKeyException exception) {
            throw new WorkSlugConflictException();
        }
        return view(work, true, false);
    }

    /**
     * 更新正文。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>works.updateBody</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param markdown Markdown；类型为 <code>String</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    @Transactional
    public WorkVO updateBody(Long id, String markdown) {
        WorkEntity work = lock(id);
        if (markdown == null || markdown.length() > 1_000_000) {
            throw new WorkInvalidException("正文长度无效");
        }
        work.setBodyMarkdown(markdown);
        if ("PUBLISHED".equals(work.getStatus())) { validatePublish(work); }
        touch(work);
        works.updateBody(work);
        return view(work, true, false);
    }

    /**
     * 更新详情。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>details.insert</code>、<code>details.updateDetailJson</code>、<code>works.touch</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param detail 详情；类型为 <code>JsonNode</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    @Transactional
    public WorkVO updateDetail(Long id, JsonNode detail) {
        WorkEntity work = lock(id);
        validateDetail(work, detail);
        WorkDetailEntity row = detailRow(id);
        if (row == null) {
            row = new WorkDetailEntity();
            row.setWorkId(id);
            row.setWorkType(work.getWorkType());
            row.setDetailJson(detail.toString());
            details.insert(row);
        } else {
            row.setDetailJson(detail.toString());
            details.updateDetailJson(row);
        }
        touch(work);
        works.touch(work);
        return view(work, true, false);
    }

    /**
     * 添加媒体。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>media.insert</code>、<code>works.touch</code>。
     * 实现中的显式失败类型包括 <code>WorkMediaConflictException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkMediaDTO</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    @Transactional
    public WorkVO addMedia(Long id, WorkMediaDTO request) {
        WorkEntity work = lock(id);
        validateMedia(id, request, null);
        WorkMediaEntity row = new WorkMediaEntity();
        row.setWorkId(id);
        row.setMediaAssetId(request.getMediaAssetId());
        row.setUsageType(request.getUsageType().name());
        row.setCaption(optionalText(request.getCaption(), 500));
        row.setSortOrder(safeOrder(request.getSortOrder()));
        try {
            media.insert(row);
        } catch (DuplicateKeyException exception) {
            throw new WorkMediaConflictException();
        }
        attach(row);
        touch(work);
        works.touch(work);
        return view(work, true, false);
    }

    /**
     * 更新媒体。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>media.selectById</code>、<code>media.updateContent</code>、<code>works.touch</code>。
     * 实现中的显式失败类型包括 <code>WorkMediaNotFoundException</code>、<code>WorkStateInvalidException</code>、<code>WorkMediaConflictException</code>；具体触发条件见对应分支。
     *
     * @param mediaId 媒体标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkMediaDTO</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    @Transactional
    public WorkVO updateMedia(Long mediaId, WorkMediaDTO request) {
        WorkMediaEntity row = media.selectById(mediaId);
        if (row == null) {
            throw new WorkMediaNotFoundException();
        }
        WorkEntity work = lock(row.getWorkId());
        if ("PUBLISHED".equals(work.getStatus()) && "COVER".equals(row.getUsageType()) && request.getUsageType() != WorkMediaUsage.COVER) {
            throw new WorkStateInvalidException("已发布作品必须保留封面");
        }
        validateMedia(row.getWorkId(), request, row.getId());
        boolean changed = !row.getMediaAssetId().equals(request.getMediaAssetId())
                || !row.getUsageType().equals(request.getUsageType().name());
        if (changed) {
            detach(row);
        }
        row.setMediaAssetId(request.getMediaAssetId());
        row.setUsageType(request.getUsageType().name());
        row.setCaption(optionalText(request.getCaption(), 500));
        row.setSortOrder(safeOrder(request.getSortOrder()));
        try {
            media.updateContent(row);
        } catch (DuplicateKeyException exception) {
            throw new WorkMediaConflictException();
        }
        if (changed) {
            attach(row);
        }
        touch(work);
        works.touch(work);
        return view(work, true, false);
    }

    /**
     * 移除媒体。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>media.selectById</code>、<code>media.deleteById</code>、<code>works.touch</code>。
     * 实现中的显式失败类型包括 <code>WorkMediaNotFoundException</code>、<code>WorkStateInvalidException</code>；具体触发条件见对应分支。
     *
     * @param mediaId 媒体标识；类型为 <code>Long</code>
     */
    @Override
    @Transactional
    public void removeMedia(Long mediaId) {
        WorkMediaEntity row = media.selectById(mediaId);
        if (row == null) {
            throw new WorkMediaNotFoundException();
        }
        WorkEntity work = lock(row.getWorkId());
        if ("PUBLISHED".equals(work.getStatus()) && "COVER".equals(row.getUsageType())) {
            throw new WorkStateInvalidException("已发布作品请更换封面，或先撤回后移除");
        }
        media.deleteById(mediaId);
        detach(row);
        touch(work);
        works.touch(work);
    }

    /**
     * 顺序媒体处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>media.updateSortOrder</code>、<code>works.touch</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>IdOrderDTO</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    @Transactional
    public WorkVO orderMedia(Long id, IdOrderDTO request) {
        WorkEntity work = lock(id);
        List<WorkMediaEntity> rows = mediaRows(id);
        checkOrder(request.getIds(), rows.stream().map(WorkMediaEntity::getId).toList());
        for (int index = 0; index < request.getIds().size(); index++) {
            final Long target = request.getIds().get(index);
            WorkMediaEntity row = rows.stream().filter(item -> item.getId().equals(target))
                    .findFirst().orElseThrow(WorkMediaNotFoundException::new);
            row.setSortOrder(index);
            media.updateSortOrder(row);
        }
        touch(work);
        works.touch(work);
        return view(work, true, false);
    }

    /**
     * 添加链接。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>links.insert</code>、<code>works.touch</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkLinkDTO</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    @Transactional
    public WorkVO addLink(Long id, WorkLinkDTO request) {
        WorkEntity work = lock(id);
        WorkLinkEntity row = new WorkLinkEntity();
        fillLink(row, request);
        row.setWorkId(id);
        links.insert(row);
        touch(work);
        works.touch(work);
        return view(work, true, false);
    }

    /**
     * 更新链接。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>links.selectById</code>、<code>links.updateContent</code>、<code>works.touch</code>。
     * 实现中的显式失败类型包括 <code>WorkLinkNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param linkId 链接标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkLinkDTO</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    @Transactional
    public WorkVO updateLink(Long linkId, WorkLinkDTO request) {
        WorkLinkEntity row = links.selectById(linkId);
        if (row == null) {
            throw new WorkLinkNotFoundException();
        }
        WorkEntity work = lock(row.getWorkId());
        fillLink(row, request);
        links.updateContent(row);
        touch(work);
        works.touch(work);
        return view(work, true, false);
    }

    /**
     * 移除链接。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>links.selectById</code>、<code>links.deleteById</code>、<code>works.touch</code>。
     * 实现中的显式失败类型包括 <code>WorkLinkNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param linkId 链接标识；类型为 <code>Long</code>
     */
    @Override
    @Transactional
    public void removeLink(Long linkId) {
        WorkLinkEntity row = links.selectById(linkId);
        if (row == null) {
            throw new WorkLinkNotFoundException();
        }
        WorkEntity work = lock(row.getWorkId());
        links.deleteById(linkId);
        touch(work);
        works.touch(work);
    }

    /**
     * 顺序Links处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>links.updateSortOrder</code>、<code>works.touch</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>IdOrderDTO</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    @Transactional
    public WorkVO orderLinks(Long id, IdOrderDTO request) {
        WorkEntity work = lock(id);
        List<WorkLinkEntity> rows = linkRows(id);
        checkOrder(request.getIds(), rows.stream().map(WorkLinkEntity::getId).toList());
        for (int index = 0; index < request.getIds().size(); index++) {
            final Long target = request.getIds().get(index);
            WorkLinkEntity row = rows.stream().filter(item -> item.getId().equals(target))
                    .findFirst().orElseThrow(WorkLinkNotFoundException::new);
            row.setSortOrder(index);
            links.updateSortOrder(row);
        }
        touch(work);
        works.touch(work);
        return view(work, true, false);
    }

    /**
     * 发布作品作品。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>works.publish</code>。
     * 实现中的显式失败类型包括 <code>WorkStateInvalidException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    @Transactional
    public WorkVO publish(Long id) {
        WorkEntity work = lock(id);
        if (!WorkStatus.DRAFT.name().equals(work.getStatus())) {
            throw new WorkStateInvalidException("只有草稿可首次发布；已撤回作品请使用恢复");
        }
        validatePublish(work);
        work.setStatus(WorkStatus.PUBLISHED.name());
        work.setPublishedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        touch(work);
        works.publish(work);
        return view(work, true, false);
    }

    /**
     * 撤回作品作品。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>works.withdraw</code>、<code>events.afterCommit</code>。
     * 实现中的显式失败类型包括 <code>WorkStateInvalidException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    @Transactional
    public WorkVO withdraw(Long id) {
        WorkEntity work = lock(id);
        if (!WorkStatus.PUBLISHED.name().equals(work.getStatus())) {
            throw new WorkStateInvalidException("只有已发布作品可撤回");
        }
        work.setStatus(WorkStatus.WITHDRAWN.name());
        work.setWithdrawnAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        touch(work);
        works.withdraw(work);
        events.afterCommit(new WorkPublicationChangedEvent(work.getId(), work.getSlug(), false));
        return view(work, true, false);
    }

    /**
     * 恢复作品作品。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>works.restore</code>。
     * 实现中的显式失败类型包括 <code>WorkStateInvalidException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    @Override
    @Transactional
    public WorkVO restore(Long id) {
        WorkEntity work = lock(id);
        if (!WorkStatus.WITHDRAWN.name().equals(work.getStatus())) {
            throw new WorkStateInvalidException("只有已撤回作品可恢复");
        }
        validatePublish(work);
        work.setStatus(WorkStatus.PUBLISHED.name());
        touch(work);
        works.restore(work);
        return view(work, true, false);
    }

    /**
     * 删除作品作品。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>media.deleteById</code>、<code>links.deleteById</code>、<code>details.deleteById</code>、<code>mediaReferences.detachAll</code>、<code>content.deleteAll</code>。
     * 实现中的显式失败类型包括 <code>WorkStateInvalidException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     */
    @Override
    @Transactional
    public void delete(Long id) {
        WorkEntity work = lock(id);
        if (WorkStatus.PUBLISHED.name().equals(work.getStatus())) {
            throw new WorkStateInvalidException("请先撤回作品再删除");
        }
        for (WorkMediaEntity row : mediaRows(id)) {
            detach(row);
            media.deleteById(row.getId());
        }
        for (WorkLinkEntity row : linkRows(id)) {
            links.deleteById(row.getId());
        }
        WorkDetailEntity detail = detailRow(id);
        if (detail != null) {
            details.deleteById(detail.getId());
        }
        mediaReferences.detachAll("PORTFOLIO", "WORK_PROTOTYPE", id);
        content.deleteAll(id);
        works.deleteById(id);
        events.afterCommit(new WorkPublicationChangedEvent(id, work.getSlug(), false));
    }

    /**
     * 列出作品作品。
     * 主要协作调用：<code>works.countByFilter</code>、<code>works.pageByFilter</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param type 类型；类型为 <code>WorkType</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @param keyword 关键词；类型为 <code>String</code>
     * @param publicOnly 公开Only；类型为 <code>boolean</code>
     * @return 类型为 <code>PageResult&lt;WorkVO&gt;</code> 的处理结果
     */
    private PageResult<WorkVO> list(int page, int pageSize, WorkType type,
                                     String status, String keyword, boolean publicOnly) {
        if (page < 1 || page > 100_000 || pageSize < 1 || pageSize > 100) {
            throw new WorkInvalidException("分页参数无效");
        }
        String workType = type == null ? null : type.name();
        String filterStatus = status == null || status.isBlank() ? null : status;
        String filterKeyword = null;
        if (keyword != null && !keyword.isBlank()) {
            if (keyword.length() > 100) {
                throw new WorkInvalidException("搜索词过长");
            }
            filterKeyword = keyword.trim();
        }
        long total = works.countByFilter(workType, filterStatus, filterKeyword);
        List<WorkVO> items = works
                .pageByFilter(workType, filterStatus, filterKeyword, pageSize, (long) (page - 1) * pageSize)
                .stream()
                .map(work -> view(work, false, publicOnly)).toList();
        return PageResult.<WorkVO>builder().items(items).total(total).page(page).pageSize(pageSize).build();
    }

    /**
     * 检查所需作品作品。
     * 主要协作调用：<code>works.selectById</code>。
     * 实现中的显式失败类型包括 <code>WorkNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>WorkEntity</code> 的处理结果
     */
    private WorkEntity require(Long id) {
        WorkEntity work = id == null ? null : works.selectById(id);
        if (work == null) {
            throw new WorkNotFoundException();
        }
        return work;
    }

    /**
     * 锁处理入口。
     * 主要协作调用：<code>works.byIdForUpdate</code>。
     * 实现中的显式失败类型包括 <code>WorkNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>WorkEntity</code> 的处理结果
     */
    private WorkEntity lock(Long id) {
        WorkEntity work = id == null ? null : works.byIdForUpdate(id);
        if (work == null) {
            throw new WorkNotFoundException();
        }
        return work;
    }

    /**
     * 读取详情行。
     * 主要协作调用：<code>details.selectByWorkId</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @return 类型为 <code>WorkDetailEntity</code> 的处理结果
     */
    private WorkDetailEntity detailRow(Long workId) {
        return details.selectByWorkId(workId);
    }

    /**
     * 媒体行集合处理入口。
     * 主要协作调用：<code>media.listByWorkId</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    private List<WorkMediaEntity> mediaRows(Long workId) {
        return media.listByWorkId(workId);
    }

    /**
     * 链接行集合处理入口。
     * 主要协作调用：<code>links.listByWorkId</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    private List<WorkLinkEntity> linkRows(Long workId) {
        return links.listByWorkId(workId);
    }

    /**
     * view处理入口。
     * 主要协作调用：<code>mediaAssets.get</code>、<code>objectMapper.readTree</code>、<code>content.taxonomy</code>、<code>content.tags</code>、<code>works.previousPublished</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param work 作品；类型为 <code>WorkEntity</code>
     * @param full 完整；类型为 <code>boolean</code>
     * @param publicOnly 公开Only；类型为 <code>boolean</code>
     * @return 类型为 <code>WorkVO</code> 的处理结果
     */
    private WorkVO view(WorkEntity work, boolean full, boolean publicOnly) {
        List<WorkMediaEntity> attached = mediaRows(work.getId());
        String coverUrl = attached.stream()
                .filter(row -> WorkMediaUsage.COVER.name().equals(row.getUsageType()))
                .map(row -> mediaAssets.get(row.getMediaAssetId()))
                .filter(asset -> asset != null && "ACTIVE".equals(asset.getStatus()) && (!publicOnly || "PUBLIC".equals(asset.getAccessLevel())))
                .map(MediaAssetSummary::getContentUrl).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        WorkVO result = WorkVO.builder().id(String.valueOf(work.getId())).slug(work.getSlug())
                .workType(work.getWorkType()).title(work.getTitle()).summary(work.getSummary())
                .status(work.getStatus()).coverUrl(coverUrl).publishedAt(work.getPublishedAt())
                .updatedAt(work.getUpdatedAt()).build();
        WorkDetailEntity detail = detailRow(work.getId());
        if (detail != null) {
            try {
                result.setTypeDetail(objectMapper.readTree(detail.getDetailJson()));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Stored portfolio detail is invalid", exception);
            }
        }
        result.setPrototypeAssetId(work.getPrototypeAssetId() == null ? null : work.getPrototypeAssetId().toString());
        result.setPrototypeEntry(work.getPrototypeEntry());
        result.setPrototypeUrl(work.getPrototypeAssetId() == null ? null : "/api/public/portfolio/works/" + work.getSlug() + "/live/" + work.getPrototypeEntry());
        result.setSubtitle(work.getSubtitle()); result.setCategoryId(work.getCategoryId());
        result.setFormatId(work.getFormatId()); result.setRole(work.getRole()); result.setTechStack(work.getTechStack());
        result.setProjectStatus(work.getProjectStatus()); result.setStartedOn(work.getStartedOn()); result.setEndedOn(work.getEndedOn());
        result.setFeatured(work.getFeatured()); result.setSortOrder(work.getSortOrder());
        result.setSeoTitle(work.getSeoTitle()); result.setSeoDescription(work.getSeoDescription());
        var taxonomy = content.taxonomy();
        result.setCategory(taxonomy.get("categories").stream().filter(t -> t.getId().equals(String.valueOf(work.getCategoryId()))).findFirst().orElse(null));
        result.setFormat(taxonomy.get("formats").stream().filter(t -> t.getId().equals(String.valueOf(work.getFormatId()))).findFirst().orElse(null));
        result.setTags(content.tags(work.getId()));
        if (full) {
            if (publicOnly) {
                result.setPreviousWork(works.previousPublished(work.getId(), work.getSortOrder()));
                result.setNextWork(works.nextPublished(work.getId(), work.getSortOrder()));
            }
            var allSections = content.sections(work.getId(), false);
            result.setHasSections(!allSections.isEmpty());
            result.setSections(publicOnly ? content.sections(work.getId(), true) : allSections);
            result.setSearchableText(content.searchableText(work.getId(), work.getBodyMarkdown()) + "\n"
                    + (work.getSubtitle() == null ? "" : work.getSubtitle()) + " "
                    + (work.getRole() == null ? "" : work.getRole()) + " "
                    + (work.getTechStack() == null ? "" : work.getTechStack()) + " "
                    + (result.getCategory() == null ? "" : result.getCategory().getName()) + " "
                    + (result.getFormat() == null ? "" : result.getFormat().getName()) + " "
                    + result.getTags().stream().map(com.starrainnotes.portfolio.vo.WorkTaxonomyVO::getName).collect(java.util.stream.Collectors.joining(" ")));
            // Once blocks exist, even all-hidden blocks cannot expose the old body.
            result.setBodyMarkdown(publicOnly && !allSections.isEmpty() ? "" : work.getBodyMarkdown());
            result.setMedia(attached.stream().filter(row -> {
                MediaAssetSummary asset = mediaAssets.get(row.getMediaAssetId());
                return !publicOnly || (asset != null && "ACTIVE".equals(asset.getStatus()) && "PUBLIC".equals(asset.getAccessLevel()));
            }).map(this::mediaView).toList());
            result.setLinks(linkRows(work.getId()).stream()
                    .filter(row -> !publicOnly || "ENABLED".equals(row.getStatus()))
                    .map(this::linkView).toList());
        }
        return result;
    }

    /**
     * 媒体View处理入口。
     * 主要协作调用：<code>mediaAssets.get</code>。
     *
     * @param row 行；类型为 <code>WorkMediaEntity</code>
     * @return 类型为 <code>WorkMediaVO</code> 的处理结果
     */
    private WorkMediaVO mediaView(WorkMediaEntity row) {
        MediaAssetSummary asset = mediaAssets.get(row.getMediaAssetId());
        return WorkMediaVO.builder().id(String.valueOf(row.getId()))
                .mediaAssetId(String.valueOf(row.getMediaAssetId()))
                .usageType(row.getUsageType()).caption(row.getCaption())
                .sortOrder(row.getSortOrder()).url(asset == null ? null : asset.getContentUrl()).build();
    }

    /**
     * 链接View处理入口。
     * 关键处理步骤调用 <code>WorkLinkVO.builder</code>、<code>String.valueOf</code>、<code>row.getId</code>、<code>row.getLinkType</code>。
     *
     * @param row 行；类型为 <code>WorkLinkEntity</code>
     * @return 类型为 <code>WorkLinkVO</code> 的处理结果
     */
    private WorkLinkVO linkView(WorkLinkEntity row) {
        return WorkLinkVO.builder().id(String.valueOf(row.getId())).linkType(row.getLinkType())
                .label(row.getLabel()).url(row.getUrl()).sortOrder(row.getSortOrder())
                .enabled("ENABLED".equals(row.getStatus())).build();
    }

    /**
     * 校验详情。
     * 主要协作调用：<code>validators.stream</code>。
     *
     * @param work 作品；类型为 <code>WorkEntity</code>
     * @param detail 详情；类型为 <code>JsonNode</code>
     */
    private void validateDetail(WorkEntity work, JsonNode detail) {
        WorkType type = WorkType.valueOf(work.getWorkType());
        validators.stream().filter(validator -> validator.supports() == type)
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing work detail validator"))
                .validate(detail);
    }

    /**
     * 校验发布。
     * 主要协作调用：<code>content.sections</code>、<code>content.validateTaxonomy</code>、<code>content.validatePublication</code>、<code>mediaAssets.get</code>、<code>mediaAssets.assertUsable</code>。
     * 实现中的显式失败类型包括 <code>WorkStateInvalidException</code>；具体触发条件见对应分支。
     *
     * @param work 作品；类型为 <code>WorkEntity</code>
     */
    private void validatePublish(WorkEntity work) {
        if (work.getSummary() == null || work.getSummary().isBlank()
                || work.getCategoryId() == null || work.getFormatId() == null) {
            throw new WorkStateInvalidException("发布前请补全摘要、分类与形态");
        }
        if (content.sections(work.getId(), false).isEmpty()
                && (work.getBodyMarkdown() == null || work.getBodyMarkdown().isBlank())) {
            throw new WorkStateInvalidException("发布前需要正文或可见内容区块");
        }
        content.validateTaxonomy(work.getCategoryId(), work.getFormatId());
        content.validatePublication(work.getId());
        boolean cover = mediaRows(work.getId()).stream().anyMatch(row -> WorkMediaUsage.COVER.name().equals(row.getUsageType()));
        if (!cover) { throw new WorkStateInvalidException("发布前请选择公开封面"); }
        for (WorkMediaEntity row : mediaRows(work.getId())) {
            MediaAssetSummary asset = mediaAssets.get(row.getMediaAssetId());
            if (asset == null || !"PUBLIC".equals(asset.getAccessLevel())) {
                throw new WorkStateInvalidException("发布作品的媒体必须可公开访问");
            }
        }
        mediaRows(work.getId()).forEach(row -> mediaAssets.assertUsable(row.getMediaAssetId()));
        linkRows(work.getId()).forEach(row -> WorkLinkRules.requireSafeUrl(row.getUrl()));
    }


    /**
     * 已通过元数据校验的上传信息处理入口。
     * 主要协作调用：<code>content.validateTaxonomy</code>、<code>content.validateFeatured</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param work 作品；类型为 <code>WorkEntity</code>
     * @param request 本次操作的请求参数对象；类型为 <code>JsonNode</code>
     */
    private void metadata(WorkEntity work, JsonNode request) {
        if (request.hasNonNull("subtitle")) { work.setSubtitle(optionalText(request.get("subtitle").asText(), 255)); }
        if (request.hasNonNull("role")) { work.setRole(optionalText(request.get("role").asText(), 255)); }
        if (request.hasNonNull("techStack")) { work.setTechStack(optionalText(request.get("techStack").asText(), 1000)); }
        if (request.hasNonNull("seoTitle")) { work.setSeoTitle(optionalText(request.get("seoTitle").asText(), 255)); }
        if (request.hasNonNull("seoDescription")) { work.setSeoDescription(optionalText(request.get("seoDescription").asText(), 1000)); }
        if (request.hasNonNull("categoryId")) { work.setCategoryId(request.get("categoryId").asLong()); }
        if (request.hasNonNull("formatId")) { work.setFormatId(request.get("formatId").asLong()); }
        content.validateTaxonomy(work.getCategoryId(), work.getFormatId());
        if (request.hasNonNull("projectStatus")) {
            String status = request.get("projectStatus").asText();
            if (!Set.of("IDEA", "PLANNING", "DEVELOPING", "COMPLETED", "MAINTAINING", "ARCHIVED", "ONLINE").contains(status)) {
                throw new WorkInvalidException("项目生命周期无效");
            }
            work.setProjectStatus(status);
        }
        if (request.path("clearStartedOn").asBoolean(false)) { work.setStartedOn(null); }
        if (request.path("clearEndedOn").asBoolean(false)) { work.setEndedOn(null); }
        if (request.hasNonNull("startedOn")) { work.setStartedOn(java.time.LocalDate.parse(request.get("startedOn").asText())); }
        if (request.hasNonNull("endedOn")) { work.setEndedOn(java.time.LocalDate.parse(request.get("endedOn").asText())); }
        if (work.getStartedOn() != null && work.getEndedOn() != null && work.getStartedOn().isAfter(work.getEndedOn())) {
            throw new WorkInvalidException("结束日期不能早于开始日期");
        }
        if (request.hasNonNull("sortOrder")) { work.setSortOrder(safeOrder(request.get("sortOrder").asInt())); }
        if (request.hasNonNull("featured")) {
            content.validateFeatured(work.getId() == null ? -1L : work.getId(), request.get("featured").asBoolean());
            work.setFeatured(request.get("featured").asBoolean());
        }
    }

    /**
     * filtered作品列表处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>works.countByTaxonomy</code>、<code>works.pageByTaxonomy</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param type 类型；类型为 <code>WorkType</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @param keyword 关键词；类型为 <code>String</code>
     * @param filter 筛选；类型为 <code>com.starrainnotes.portfolio.dto.WorkFilterDTO</code>
     * @param publicOnly 公开Only；类型为 <code>boolean</code>
     * @return 类型为 <code>PageResult&lt;WorkVO&gt;</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkVO> filteredWorks(int page, int pageSize, WorkType type, String status,
            String keyword, com.starrainnotes.portfolio.dto.WorkFilterDTO filter, boolean publicOnly) {
        if (page < 1 || page > 100000 || pageSize < 1 || pageSize > 100) { throw new WorkInvalidException("分页参数无效"); }
        String state = publicOnly ? "PUBLISHED" : status;
        if (state != null && !Set.of("DRAFT", "PUBLISHED", "WITHDRAWN").contains(state)) { throw new WorkInvalidException("作品状态无效"); }
        String q = optionalText(keyword, 100);
        long total = works.countByTaxonomy(type == null ? null : type.name(), state, q, filter);
        var items = works.pageByTaxonomy(type == null ? null : type.name(), state, q, filter, pageSize, (long)(page - 1) * pageSize)
                .stream().map(row -> view(row, false, publicOnly)).toList();
        return PageResult.<WorkVO>builder().items(items).total(total).page(page).pageSize(pageSize).build();
    }

    /**
     * uniqueGenerated固定链接标识处理入口。
     * 主要协作调用：<code>works.countBySlug</code>。
     * 实现中的显式失败类型包括 <code>WorkSlugConflictException</code>；具体触发条件见对应分支。
     *
     * @param title 标题；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String uniqueGeneratedSlug(String title) {
        String base = WorkSlugRules.fromTitle(title);
        for (int suffix = 1; suffix <= 100; suffix++) {
            String candidate = suffix == 1 ? base : base + "-" + suffix;
            if (works.countBySlug(candidate) == 0) {
                return candidate;
            }
        }
        throw new WorkSlugConflictException();
    }

    /**
     * 必需文本处理入口。
     * 关键处理步骤调用 <code>raw.trim</code>、<code>value.isEmpty</code>、<code>value.length</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param raw 原始；类型为 <code>String</code>
     * @param max 最大；类型为 <code>int</code>
     * @param name 名称；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String requiredText(String raw, int max, String name) {
        String value = raw == null ? null : raw.trim();
        if (value == null || value.isEmpty() || value.length() > max) {
            throw new WorkInvalidException(name + "长度无效");
        }
        return value;
    }

    /**
     * optional文本处理入口。
     * 关键处理步骤调用 <code>raw.trim</code>、<code>value.length</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param raw 原始；类型为 <code>String</code>
     * @param max 最大；类型为 <code>int</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String optionalText(String raw, int max) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.length() > max) {
            throw new WorkInvalidException("内容过长");
        }
        return value;
    }

    /**
     * 安全顺序处理入口。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>Integer</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    private int safeOrder(Integer value) {
        if (value == null) {
            return 0;
        }
        if (value < 0 || value > 100_000) {
            throw new WorkInvalidException("排序值无效");
        }
        return value;
    }

    /**
     * 检查顺序。
     * 关键处理步骤调用 <code>submitted.size</code>、<code>existing.size</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param submitted 提交；类型为 <code>List&lt;Long&gt;</code>
     * @param existing existing；类型为 <code>List&lt;Long&gt;</code>
     */
    private void checkOrder(List<Long> submitted, List<Long> existing) {
        if (submitted == null || submitted.size() != existing.size()
                || new HashSet<>(submitted).size() != submitted.size()
                || !new HashSet<>(submitted).equals(new HashSet<>(existing))) {
            throw new WorkInvalidException("请提交当前作品的完整排序");
        }
    }

    /**
     * fill链接处理入口。
     * 关键处理步骤调用 <code>requiredText</code>、<code>request.getLinkType</code>、<code>Set.of</code>、<code>row.setLinkType</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param row 行；类型为 <code>WorkLinkEntity</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkLinkDTO</code>
     */
    private void fillLink(WorkLinkEntity row, WorkLinkDTO request) {
        String type = requiredText(request.getLinkType(), 30, "链接类型").toUpperCase();
        if (!Set.of("LIVE", "DEMO", "GITHUB", "DOWNLOAD", "VIDEO", "DOC", "ARTICLE", "BILIBILI", "DOUYIN", "OTHER").contains(type)) {
            throw new WorkInvalidException("链接类型无效");
        }
        row.setLinkType(type);
        row.setLabel(requiredText(request.getLabel(), 100, "链接名称"));
        row.setUrl(WorkLinkRules.requireSafeUrl(request.getUrl()));
        row.setSortOrder(safeOrder(request.getSortOrder()));
        row.setStatus(Boolean.FALSE.equals(request.getEnabled()) ? "DISABLED" : "ENABLED");
    }

    /**
     * 校验媒体。
     * 主要协作调用：<code>mediaAssets.assertUsable</code>、<code>mediaAssets.get</code>、<code>works.selectById</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>、<code>WorkMediaConflictException</code>；具体触发条件见对应分支。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkMediaDTO</code>
     * @param excludeId exclude标识；类型为 <code>Long</code>
     */
    private void validateMedia(Long workId, WorkMediaDTO request, Long excludeId) {
        mediaAssets.assertUsable(request.getMediaAssetId());
        MediaAssetSummary asset = mediaAssets.get(request.getMediaAssetId());
        if (asset == null) {
            throw new WorkInvalidException("媒体不存在");
        }
        WorkEntity owner = works.selectById(workId);
        if (owner != null && "PUBLISHED".equals(owner.getStatus()) && !"PUBLIC".equals(asset.getAccessLevel())) {
            throw new WorkInvalidException("已发布作品只能引用公开媒体");
        }
        WorkMediaUsage usage = request.getUsageType();
        if ((usage == WorkMediaUsage.COVER || usage == WorkMediaUsage.SCREENSHOT)
                && !"IMAGE".equals(asset.getMediaType())) {
            throw new WorkInvalidException("封面与截图只能使用图片");
        }
        if (usage == WorkMediaUsage.AUDIO && !"AUDIO".equals(asset.getMediaType())) {
            throw new WorkInvalidException("音频用途只能使用音频文件");
        }
        if (usage == WorkMediaUsage.COVER && mediaRows(workId).stream()
                .anyMatch(row -> WorkMediaUsage.COVER.name().equals(row.getUsageType())
                        && !row.getId().equals(excludeId))) {
            throw new WorkMediaConflictException();
        }
    }

    /**
     * 建立关联作品作品。
     * 主要协作调用：<code>mediaReferences.attach</code>。
     *
     * @param row 行；类型为 <code>WorkMediaEntity</code>
     */
    private void attach(WorkMediaEntity row) {
        mediaReferences.attach(reference(row));
    }

    /**
     * 解除关联作品作品。
     * 主要协作调用：<code>mediaReferences.detach</code>。
     *
     * @param row 行；类型为 <code>WorkMediaEntity</code>
     */
    private void detach(WorkMediaEntity row) {
        mediaReferences.detach(reference(row));
    }

    /**
     * 引用处理入口。
     * 关键处理步骤调用 <code>MediaReferenceCommand.builder</code>、<code>row.getMediaAssetId</code>、<code>row.getId</code>、<code>WorkMediaUsage.valueOf</code>。
     *
     * @param row 行；类型为 <code>WorkMediaEntity</code>
     * @return 类型为 <code>MediaReferenceCommand</code> 的处理结果
     */
    private MediaReferenceCommand reference(WorkMediaEntity row) {
        return MediaReferenceCommand.builder().mediaAssetId(row.getMediaAssetId())
                .sourceModule(SOURCE_MODULE).sourceType(SOURCE_TYPE).sourceId(row.getId())
                .usageCode(WorkMediaUsage.valueOf(row.getUsageType()).usageCode()).build();
    }

    /**
     * touch处理入口。
     * 主要协作调用：<code>actor.current</code>、<code>events.afterCommit</code>。
     *
     * @param work 作品；类型为 <code>WorkEntity</code>
     */
    private void touch(WorkEntity work) {
        work.setUpdatedByAccountId(actor.current().getAccountId());
        work.setUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        if ("PUBLISHED".equals(work.getStatus())) {
            events.afterCommit(WorkPublicationChangedEvent.builder().workId(work.getId()).slug(work.getSlug())
                    .published(true).build());
        }
    }
}
