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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioContentServiceImpl implements PortfolioContentService {
    private final WorkContentMapper content;
    private final WorkMapper works;
    private final MediaAssetApi assets;
    private final MediaReferenceApi references;
    private final CurrentActorApi actor;
    private final WorkEventPublisher events;
    private final ObjectMapper json;

    @Override public Map<String, List<WorkTaxonomyVO>> taxonomy() {
        return Map.of("categories", content.categories().stream().map(this::taxon).toList(),
                "formats", content.formats().stream().map(this::taxon).toList(),
                "tags", content.tags().stream().map(this::taxon).toList());
    }
    @Override public List<WorkTemplateVO> templates() {
        return content.templates().stream().map(row -> WorkTemplateVO.builder()
                .id(row.getId().toString()).code(row.getCode()).name(row.getName())
                .description(row.getDescription()).sections(parse(row.getSectionsJson())).build()).toList();
    }
    @Override public List<WorkTaxonomyVO> tags(Long workId) {
        return content.workTags(workId).stream().map(this::taxon).toList();
    }
    @Override public List<WorkSectionVO> sections(Long workId, boolean publicOnly) {
        return content.sections(workId).stream().filter(s -> !publicOnly || Boolean.TRUE.equals(s.getVisible()))
                .map(s -> view(s, publicOnly)).toList();
    }
    @Override @Transactional public void setTags(Long workId, List<Long> ids) {
        if (ids == null) { return; }
        Set<Long> available = new HashSet<>(content.tags().stream().map(WorkTaxonomyEntity::getId).toList());
        if (ids.size() > 30 || new HashSet<>(ids).size() != ids.size() || !available.containsAll(ids)) {
            throw new WorkInvalidException("标签不存在、重复或超过 30 个");
        }
        content.clearTags(workId);
        ids.forEach(id -> content.addTag(workId, id));
    }
    @Override public void validateTaxonomy(Long categoryId, Long formatId) {
        if (categoryId != null && content.categories().stream().noneMatch(t -> t.getId().equals(categoryId))) {
            throw new WorkInvalidException("作品分类不存在");
        }
        if (formatId != null && content.formats().stream().noneMatch(t -> t.getId().equals(formatId))) {
            throw new WorkInvalidException("作品形态不存在");
        }
    }
    @Override @Transactional public void validateFeatured(Long workId, boolean featured) {
        if (!featured) { return; }
        // One shared row serializes the cap across edits to different works.
        content.lockFeatured();
        if (content.featuredCount(workId) >= 3) { throw new WorkInvalidException("精选作品最多 3 个"); }
    }
    @Override @Transactional public void copyTemplate(Long workId, Long templateId) {
        if (templateId == null) { return; }
        WorkTemplateEntity template = content.template(templateId);
        if (template == null) { throw new WorkInvalidException("作品模板不存在"); }
        for (JsonNode block : parse(template.getSectionsJson())) {
            try { create(workId, json.treeToValue(block, WorkSectionDTO.class)); }
            catch (JsonProcessingException ex) { throw new IllegalStateException("Invalid template schema", ex); }
        }
    }
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
    @Override @Transactional public void delete(Long workId, Long sectionId) {
        WorkEntity work = lock(workId);
        owned(workId, sectionId);
        clearMedia(sectionId);
        content.deleteSection(sectionId);
        if ("PUBLISHED".equals(work.getStatus())) { validatePublication(workId); }
        changed(work);
    }
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
    @Override @Transactional public void deleteAll(Long workId) {
        for (WorkSectionEntity row : content.sections(workId)) {
            clearMedia(row.getId()); content.deleteSection(row.getId());
        }
        content.clearTags(workId);
    }
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
    @Override public String searchableText(Long workId, String legacy) {
        return text(workId, legacy, false);
    }
    @Override public String seoMarkdown(Long workId, String legacy) { return text(workId, legacy, true); }

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
    private String escape(String text) { return text == null ? "" : text.replace("[", "\\[").replace("]", "\\]"); }
    private void fill(WorkSectionEntity row, WorkSectionDTO request, boolean published) {
        WorkSectionRules.validate(request);
        boolean visible = !Boolean.FALSE.equals(request.getVisible());
        if (published && visible) { WorkSectionRules.complete(request); }
        validateMedia(request, published && visible);
        row.setSectionType(request.getSectionType()); row.setTitle(request.getTitle());
        row.setContent(request.getContent()); row.setDataJson(request.getData() == null ? "{}" : request.getData().toString());
        row.setBlockVersion(1); row.setVisible(visible);
    }
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
    private void clearMedia(Long sectionId) {
        content.sectionMedia(sectionId).forEach(m -> references.detach(reference(m)));
        content.clearSectionMedia(sectionId);
    }
    private MediaReferenceCommand reference(WorkSectionMediaEntity m) {
        return MediaReferenceCommand.builder().mediaAssetId(m.getMediaAssetId()).sourceModule("PORTFOLIO")
                .sourceType("WORK_SECTION_MEDIA").sourceId(m.getId()).usageCode(com.starrainnotes.media.api.constant.MediaUsageCodes.PORTFOLIO_SECTION).build();
    }
    private WorkEntity lock(Long id) {
        WorkEntity work = id == null ? null : works.byIdForUpdate(id);
        if (work == null) { throw new WorkNotFoundException(); }
        return work;
    }
    private WorkSectionEntity owned(Long workId, Long id) {
        WorkSectionEntity row = content.section(id);
        if (row == null || !row.getWorkId().equals(workId)) { throw new WorkNotFoundException(); }
        return row;
    }
    private void changed(WorkEntity work) {
        work.setUpdatedByAccountId(actor.current().getAccountId());
        work.setUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)); works.touch(work);
        if ("PUBLISHED".equals(work.getStatus())) {
            events.afterCommit(WorkPublicationChangedEvent.builder().workId(work.getId()).slug(work.getSlug())
                    .published(true).build());
        }
    }
    private WorkSectionDTO dto(WorkSectionEntity row) {
        return WorkSectionDTO.builder().sectionType(row.getSectionType()).title(row.getTitle())
                .content(row.getContent()).data(parse(row.getDataJson())).blockVersion(row.getBlockVersion())
                .visible(row.getVisible()).build();
    }
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
    private WorkTaxonomyVO taxon(WorkTaxonomyEntity row) {
        return WorkTaxonomyVO.builder().id(row.getId().toString()).code(row.getCode()).name(row.getName()).groupCode(row.getGroupCode()).build();
    }
    private JsonNode parse(String raw) {
        try { return json.readTree(raw == null ? "{}" : raw); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Invalid stored block", ex); }
    }
}
