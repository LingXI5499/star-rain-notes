/*
 * PortfolioContentServiceImpl：作品模块的业务实现文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：CurrentActorApi、MediaAssetApi、MediaReferenceApi、MediaAssetSummary、MediaReferenceCommand、WorkPublicationChangedEvent、SectionMediaDTO。
 * 阅读入口：taxonomy（taxonomy处理入口）；templates（templates处理入口）；tags（标签列表处理入口）；sections（sections处理入口）。
 */

package com.starrainnotes.portfolio.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.media.api.MediaAssetApi;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaAssetSummary;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.portfolio.api.event.WorkPublicationChangedEvent;
import com.starrainnotes.portfolio.dto.SectionMediaDTO;
import com.starrainnotes.portfolio.dto.WorkSectionDTO;
import com.starrainnotes.portfolio.entity.*;
import com.starrainnotes.portfolio.event.WorkEventPublisher;
import com.starrainnotes.portfolio.exception.WorkInvalidException;
import com.starrainnotes.portfolio.exception.WorkNotFoundException;
import com.starrainnotes.portfolio.mapper.WorkContentMapper;
import com.starrainnotes.portfolio.mapper.WorkMapper;
import com.starrainnotes.portfolio.service.PortfolioContentService;
import com.starrainnotes.portfolio.validator.WorkSectionRules;
import com.starrainnotes.portfolio.vo.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 作品内容业务服务实现，属于作品模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioContentServiceImpl implements PortfolioContentService {
    /**
     * 内容。
     * 协作类型：<code>WorkContentMapper</code>。
     */
    private final WorkContentMapper content;
    /**
     * 作品列表。
     * 协作类型：<code>WorkMapper</code>。
     */
    private final WorkMapper works;
    /**
     * assets。
     * 协作类型：<code>MediaAssetApi</code>。
     */
    private final MediaAssetApi assets;
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
     * JSON 读写或映射协作对象。
     * 协作类型：<code>ObjectMapper</code>。
     */
    private final ObjectMapper json;

    /**
     * taxonomy处理入口。
     * 主要协作调用：<code>content.categories</code>、<code>content.formats</code>、<code>content.tags</code>。
     * @return 类型为 <code>Map&lt;String, List&lt;WorkTaxonomyVO&gt;&gt;</code> 的处理结果
     */
    @Override public Map<String, List<WorkTaxonomyVO>> taxonomy() {
        return Map.of("categories", content.categories().stream().map(this::taxon).toList(),
                "formats", content.formats().stream().map(this::taxon).toList(),
                "tags", content.tags().stream().map(this::taxon).toList());
    }
    /**
     * templates处理入口。
     * 主要协作调用：<code>content.templates</code>。
     * @return 本次处理得到的条目集合
     */
    @Override public List<WorkTemplateVO> templates() {
        return content.templates().stream().map(row -> WorkTemplateVO.builder()
                .id(row.getId().toString()).code(row.getCode()).name(row.getName())
                .description(row.getDescription()).sections(parse(row.getSectionsJson())).build()).toList();
    }
    /**
     * 标签列表处理入口。
     * 主要协作调用：<code>content.workTags</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    @Override public List<WorkTaxonomyVO> tags(Long workId) {
        return content.workTags(workId).stream().map(this::taxon).toList();
    }
    /**
     * sections处理入口。
     * 主要协作调用：<code>content.sections</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param publicOnly 公开Only；类型为 <code>boolean</code>
     * @return 本次处理得到的条目集合
     */
    @Override public List<WorkSectionVO> sections(Long workId, boolean publicOnly) {
        return content.sections(workId).stream().filter(s -> !publicOnly || Boolean.TRUE.equals(s.getVisible()))
                .map(s -> view(s, publicOnly)).toList();
    }
    /**
     * 设置标签列表。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>content.tags</code>、<code>content.clearTags</code>、<code>content.addTag</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    @Override @Transactional public void setTags(Long workId, List<Long> ids) {
        if (ids == null) { return; }
        Set<Long> available = new HashSet<>(content.tags().stream().map(WorkTaxonomyEntity::getId).toList());
        if (ids.size() > 30 || new HashSet<>(ids).size() != ids.size() || !available.containsAll(ids)) {
            throw new WorkInvalidException("标签不存在、重复或超过 30 个");
        }
        content.clearTags(workId);
        ids.forEach(id -> content.addTag(workId, id));
    }
    /**
     * 校验Taxonomy。
     * 主要协作调用：<code>content.categories</code>、<code>content.formats</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>
     * @param formatId 格式标识；类型为 <code>Long</code>
     */
    @Override public void validateTaxonomy(Long categoryId, Long formatId) {
        if (categoryId != null && content.categories().stream().noneMatch(t -> t.getId().equals(categoryId))) {
            throw new WorkInvalidException("作品分类不存在");
        }
        if (formatId != null && content.formats().stream().noneMatch(t -> t.getId().equals(formatId))) {
            throw new WorkInvalidException("作品形态不存在");
        }
    }
    /**
     * 校验精选。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>content.lockFeatured</code>、<code>content.featuredCount</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param featured 精选；类型为 <code>boolean</code>
     */
    @Override @Transactional public void validateFeatured(Long workId, boolean featured) {
        if (!featured) { return; }
        // One shared row serializes the cap across edits to different works.
        content.lockFeatured();
        if (content.featuredCount(workId) >= 3) { throw new WorkInvalidException("精选作品最多 3 个"); }
    }
    /**
     * copyTemplate处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>content.template</code>、<code>json.treeToValue</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>、<code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param templateId template标识；类型为 <code>Long</code>
     */
    @Override @Transactional public void copyTemplate(Long workId, Long templateId) {
        if (templateId == null) { return; }
        WorkTemplateEntity template = content.template(templateId);
        if (template == null) { throw new WorkInvalidException("作品模板不存在"); }
        for (JsonNode block : parse(template.getSectionsJson())) {
            try { create(workId, json.treeToValue(block, WorkSectionDTO.class)); }
            catch (JsonProcessingException ex) { throw new IllegalStateException("Invalid template schema", ex); }
        }
    }
    /**
     * 创建作品内容。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>content.sections</code>、<code>content.insertSection</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkSectionDTO</code>
     * @return 类型为 <code>WorkSectionVO</code> 的处理结果
     */
    @Override @Transactional public WorkSectionVO create(Long workId, WorkSectionDTO request) {
        WorkEntity work = lock(workId);
        List<WorkSectionEntity> rows = content.sections(workId);
        if (rows.size() >= 100) { throw new WorkInvalidException("每个作品最多 100 个区块"); }
        WorkSectionEntity row = new WorkSectionEntity();
        row.setWorkId(workId);
        row.setSortOrder(rows.stream().mapToInt(WorkSectionEntity::getSortOrder).max().orElse(0) + 10);
        fill(row, request, "PUBLISHED".equals(work.getStatus()));
        content.insertSection(row);
        saveMedia(row, request.getMedia());
        if ("PUBLISHED".equals(work.getStatus())) { validatePublication(workId); }
        changed(work);
        return view(row, false);
    }
    /**
     * 更新作品内容。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>content.updateSection</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param sectionId 区块标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkSectionDTO</code>
     * @return 类型为 <code>WorkSectionVO</code> 的处理结果
     */
    @Override @Transactional public WorkSectionVO update(Long workId, Long sectionId, WorkSectionDTO request) {
        WorkEntity work = lock(workId);
        WorkSectionEntity row = owned(workId, sectionId);
        fill(row, request, "PUBLISHED".equals(work.getStatus()));
        content.updateSection(row);
        clearMedia(row.getId());
        saveMedia(row, request.getMedia());
        if ("PUBLISHED".equals(work.getStatus())) { validatePublication(workId); }
        changed(work);
        return view(row, false);
    }
    /**
     * 删除作品内容。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>content.deleteSection</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param sectionId 区块标识；类型为 <code>Long</code>
     */
    @Override @Transactional public void delete(Long workId, Long sectionId) {
        WorkEntity work = lock(workId);
        owned(workId, sectionId);
        clearMedia(sectionId);
        content.deleteSection(sectionId);
        if ("PUBLISHED".equals(work.getStatus())) { validatePublication(workId); }
        changed(work);
    }
    /**
     * 调整顺序作品内容。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>content.sections</code>、<code>content.orderSection</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    @Override @Transactional public void reorder(Long workId, List<Long> ids) {
        WorkEntity work = lock(workId);
        List<Long> existing = content.sections(workId).stream().map(WorkSectionEntity::getId).toList();
        if (ids == null || ids.size() != existing.size() || new HashSet<>(ids).size() != ids.size()
                || !new HashSet<>(existing).equals(new HashSet<>(ids))) {
            throw new WorkInvalidException("请提交该作品的完整区块排序");
        }
        for (int i = 0; i < ids.size(); i++) { content.orderSection(ids.get(i), (i + 1) * 10); }
        changed(work);
    }
    /**
     * 删除全部。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>content.sections</code>、<code>content.deleteSection</code>、<code>content.clearTags</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     */
    @Override @Transactional public void deleteAll(Long workId) {
        for (WorkSectionEntity row : content.sections(workId)) {
            clearMedia(row.getId()); content.deleteSection(row.getId());
        }
        content.clearTags(workId);
    }
    /**
     * 校验发布。
     * 主要协作调用：<code>content.sections</code>、<code>content.sectionMedia</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     */
    @Override public void validatePublication(Long workId) {
        List<WorkSectionEntity> rows = content.sections(workId);
        // No sections means the old body contract is still authoritative.
        if (rows.isEmpty()) { return; }
        List<WorkSectionEntity> visible = rows.stream().filter(s -> Boolean.TRUE.equals(s.getVisible())).toList();
        if (visible.isEmpty()) { throw new WorkInvalidException("发布作品需要至少一个可见区块"); }
        for (WorkSectionEntity row : visible) {
            WorkSectionDTO request = dto(row);
            request.setMedia(content.sectionMedia(row.getId()).stream().map(m ->
                    SectionMediaDTO.builder().mediaAssetId(m.getMediaAssetId()).caption(m.getCaption()).build()).toList());
            WorkSectionRules.complete(request);
            validateMedia(request, true);
        }
    }
    /**
     * 可搜索文本处理入口。
     * 关键处理步骤调用 <code>text</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param legacy legacy；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override public String searchableText(Long workId, String legacy) {
        return text(workId, legacy, false);
    }
    /**
     * SEOMarkdown处理入口。
     * 关键处理步骤调用 <code>text</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param legacy legacy；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override public String seoMarkdown(Long workId, String legacy) { return text(workId, legacy, true); }

    /**
     * 文本处理入口。
     * 主要协作调用：<code>content.sections</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param legacy legacy；类型为 <code>String</code>
     * @param markdown Markdown；类型为 <code>boolean</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String text(Long workId, String legacy, boolean markdown) {
        List<WorkSectionEntity> rows = content.sections(workId);
        if (rows.isEmpty()) { return legacy == null ? "" : legacy; }
        StringBuilder result = new StringBuilder();
        for (WorkSectionVO section : sections(workId, true)) {
            if (section.getTitle() != null) { result.append(markdown ? "\n## " : "\n").append(section.getTitle()).append('\n'); }
            if (section.getContent() != null) {
                if (markdown && "CODE".equals(section.getSectionType())) {
                    result.append("\n~~~~\n").append(section.getContent()).append("\n~~~~\n");
                } else { result.append(section.getContent()).append('\n'); }
            }
            JsonNode items = section.getData().path("items");
            if (section.getData().has("author")) { result.append(section.getData().path("author").asText()).append('\n'); }
            if (items.isArray()) {
                for (JsonNode item : items) { item.fields().forEachRemaining(field -> {
                    if (!"url".equals(field.getKey())) { result.append(field.getValue().asText()).append(' '); }
                }); result.append('\n'); }
            }
            for (SectionMediaVO media : section.getMedia()) {
                    if (!markdown && media.getCaption() != null) { result.append(media.getCaption()).append('\n'); }
                    if (markdown && !Set.of("MARKDOWN", "CUSTOM").contains(section.getSectionType())) {
                    if ("IMAGE".equals(media.getMediaType()) && media.getUrl() != null) {
                        result.append("\n![").append(escape(media.getCaption())).append("](")
                                .append(media.getUrl()).append(")\n");
                    }
                    }
            }
        }
        return result.toString();
    }
    /**
     * 转义作品内容。
     * 关键处理步骤调用 <code>text.replace</code>。
     *
     * @param text 文本；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String escape(String text) { return text == null ? "" : text.replace("[", "\\[").replace("]", "\\]"); }
    /**
     * fill处理入口。
     * 关键处理步骤调用 <code>WorkSectionRules.validate</code>、<code>Boolean.FALSE.equals</code>、<code>request.getVisible</code>、<code>WorkSectionRules.complete</code>。
     *
     * @param row 行；类型为 <code>WorkSectionEntity</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkSectionDTO</code>
     * @param published 已发布；类型为 <code>boolean</code>
     */
    private void fill(WorkSectionEntity row, WorkSectionDTO request, boolean published) {
        WorkSectionRules.validate(request);
        boolean visible = !Boolean.FALSE.equals(request.getVisible());
        if (published && visible) { WorkSectionRules.complete(request); }
        validateMedia(request, published && visible);
        row.setSectionType(request.getSectionType()); row.setTitle(request.getTitle());
        row.setContent(request.getContent()); row.setDataJson(request.getData() == null ? "{}" : request.getData().toString());
        row.setBlockVersion(1); row.setVisible(visible);
    }
    /**
     * 校验媒体。
     * 主要协作调用：<code>assets.assertUsable</code>、<code>assets.get</code>。
     * 实现中的显式失败类型包括 <code>WorkInvalidException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>WorkSectionDTO</code>
     * @param publicOnly 公开Only；类型为 <code>boolean</code>
     */
    private void validateMedia(WorkSectionDTO request, boolean publicOnly) {
        List<SectionMediaDTO> media = request.getMedia() == null ? List.of() : request.getMedia();
        String type = request.getSectionType();
        boolean inlineImages = Set.of("MARKDOWN", "CUSTOM").contains(type);
        if (!Set.of("IMAGE", "GALLERY", "AUDIO").contains(type) && !inlineImages && !media.isEmpty()) {
            throw new WorkInvalidException("此区块不接受媒体附件");
        }
        if (media.size() > 50 || (!"GALLERY".equals(type) && !inlineImages && media.size() > 1)) {
            throw new WorkInvalidException("区块媒体数量无效");
        }
        Set<Long> ids = new HashSet<>();
        for (SectionMediaDTO item : media) {
            if (item == null || item.getMediaAssetId() == null || !ids.add(item.getMediaAssetId())
                    || (item.getCaption() != null && item.getCaption().length() > 500)) {
                throw new WorkInvalidException("媒体不存在、重复或说明过长");
            }
            assets.assertUsable(item.getMediaAssetId());
            MediaAssetSummary asset = assets.get(item.getMediaAssetId());
            if (asset == null || !("AUDIO".equals(type) ? "AUDIO" : "IMAGE").equals(asset.getMediaType())
                    || (publicOnly && !"PUBLIC".equals(asset.getAccessLevel()))) {
                throw new WorkInvalidException("媒体类型不匹配，或尚未开放公开访问");
            }
        }
    }
    /**
     * 保存媒体。
     * 主要协作调用：<code>content.insertSectionMedia</code>、<code>references.attach</code>。
     *
     * @param row 行；类型为 <code>WorkSectionEntity</code>
     * @param media 媒体；类型为 <code>List&lt;SectionMediaDTO&gt;</code>
     */
    private void saveMedia(WorkSectionEntity row, List<SectionMediaDTO> media) {
        if (media == null) { return; }
        int order = 0;
        for (SectionMediaDTO item : media) {
            WorkSectionMediaEntity attached = WorkSectionMediaEntity.builder().sectionId(row.getId())
                    .mediaAssetId(item.getMediaAssetId()).caption(item.getCaption()).sortOrder(order++ * 10).build();
            content.insertSectionMedia(attached);
            references.attach(reference(attached));
        }
    }
    /**
     * 清理媒体。
     * 主要协作调用：<code>content.sectionMedia</code>、<code>references.detach</code>、<code>content.clearSectionMedia</code>。
     *
     * @param sectionId 区块标识；类型为 <code>Long</code>
     */
    private void clearMedia(Long sectionId) {
        content.sectionMedia(sectionId).forEach(m -> references.detach(reference(m)));
        content.clearSectionMedia(sectionId);
    }
    /**
     * 引用处理入口。
     * 关键处理步骤调用 <code>MediaReferenceCommand.builder</code>、<code>m.getMediaAssetId</code>、<code>m.getId</code>。
     *
     * @param m m；类型为 <code>WorkSectionMediaEntity</code>
     * @return 类型为 <code>MediaReferenceCommand</code> 的处理结果
     */
    private MediaReferenceCommand reference(WorkSectionMediaEntity m) {
        return MediaReferenceCommand.builder().mediaAssetId(m.getMediaAssetId()).sourceModule("PORTFOLIO")
                .sourceType("WORK_SECTION_MEDIA").sourceId(m.getId()).usageCode(com.starrainnotes.media.api.constant.MediaUsageCodes.PORTFOLIO_SECTION).build();
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
        if (work == null) { throw new WorkNotFoundException(); }
        return work;
    }
    /**
     * owned处理入口。
     * 主要协作调用：<code>content.section</code>。
     * 实现中的显式失败类型包括 <code>WorkNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>WorkSectionEntity</code> 的处理结果
     */
    private WorkSectionEntity owned(Long workId, Long id) {
        WorkSectionEntity row = content.section(id);
        if (row == null || !row.getWorkId().equals(workId)) { throw new WorkNotFoundException(); }
        return row;
    }
    /**
     * changed处理入口。
     * 主要协作调用：<code>actor.current</code>、<code>works.touch</code>、<code>events.afterCommit</code>。
     *
     * @param work 作品；类型为 <code>WorkEntity</code>
     */
    private void changed(WorkEntity work) {
        work.setUpdatedByAccountId(actor.current().getAccountId());
        work.setUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)); works.touch(work);
        if ("PUBLISHED".equals(work.getStatus())) {
            events.afterCommit(WorkPublicationChangedEvent.builder().workId(work.getId()).slug(work.getSlug())
                    .published(true).build());
        }
    }
    /**
     * 传输数据对象处理入口。
     * 关键处理步骤调用 <code>WorkSectionDTO.builder</code>、<code>row.getSectionType</code>、<code>row.getTitle</code>、<code>row.getContent</code>。
     *
     * @param row 行；类型为 <code>WorkSectionEntity</code>
     * @return 类型为 <code>WorkSectionDTO</code> 的处理结果
     */
    private WorkSectionDTO dto(WorkSectionEntity row) {
        return WorkSectionDTO.builder().sectionType(row.getSectionType()).title(row.getTitle())
                .content(row.getContent()).data(parse(row.getDataJson())).blockVersion(row.getBlockVersion())
                .visible(row.getVisible()).build();
    }
    /**
     * view处理入口。
     * 主要协作调用：<code>content.sectionMedia</code>、<code>assets.get</code>。
     *
     * @param row 行；类型为 <code>WorkSectionEntity</code>
     * @param publicOnly 公开Only；类型为 <code>boolean</code>
     * @return 类型为 <code>WorkSectionVO</code> 的处理结果
     */
    private WorkSectionVO view(WorkSectionEntity row, boolean publicOnly) {
        List<SectionMediaVO> media = content.sectionMedia(row.getId()).stream().map(m -> {
            MediaAssetSummary asset = assets.get(m.getMediaAssetId());
            if (asset == null || !"ACTIVE".equals(asset.getStatus())
                    || (publicOnly && !"PUBLIC".equals(asset.getAccessLevel()))) { return null; }
            return SectionMediaVO.builder().mediaAssetId(m.getMediaAssetId().toString()).caption(m.getCaption())
                    .url(asset.getContentUrl()).mediaType(asset.getMediaType()).build();
        }).filter(Objects::nonNull).toList();
        return WorkSectionVO.builder().id(row.getId().toString()).sectionType(row.getSectionType())
                .title(row.getTitle()).content(row.getContent()).data(parse(row.getDataJson()))
                .visible(row.getVisible()).blockVersion(row.getBlockVersion()).sortOrder(row.getSortOrder()).media(media).build();
    }
    /**
     * taxon处理入口。
     * 关键处理步骤调用 <code>WorkTaxonomyVO.builder</code>、<code>row.getId</code>、<code>row.getCode</code>、<code>row.getName</code>。
     *
     * @param row 行；类型为 <code>WorkTaxonomyEntity</code>
     * @return 类型为 <code>WorkTaxonomyVO</code> 的处理结果
     */
    private WorkTaxonomyVO taxon(WorkTaxonomyEntity row) {
        return WorkTaxonomyVO.builder().id(row.getId().toString()).code(row.getCode()).name(row.getName()).groupCode(row.getGroupCode()).build();
    }
    /**
     * 解析作品内容。
     * 主要协作调用：<code>json.readTree</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param raw 原始；类型为 <code>String</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    private JsonNode parse(String raw) {
        try { return json.readTree(raw == null ? "{}" : raw); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Invalid stored block", ex); }
    }
}
