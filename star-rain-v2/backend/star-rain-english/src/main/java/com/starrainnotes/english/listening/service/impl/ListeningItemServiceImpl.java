package com.starrainnotes.english.listening.service.impl;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.listening.dto.ListeningItemDto.Item;
import com.starrainnotes.english.listening.dto.ListeningItemDto.Page;
import com.starrainnotes.english.listening.dto.ListeningItemDto.Request;
import com.starrainnotes.english.listening.mapper.ListeningItemMapper;
import com.starrainnotes.english.listening.media.ListeningMediaReferences;
import com.starrainnotes.english.listening.service.ListeningItemService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListeningItemServiceImpl implements ListeningItemService {
    private final ListeningItemMapper mapper;
    private final ListeningMediaReferences mediaReferences;

    @Override
    @Transactional(readOnly = true)
    public Page list(boolean admin, String search, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(50, size));
        String term = search == null ? "" : search.trim();
        long total = mapper.count(!admin, term);
        List<Item> items = total == 0 ? List.of()
                : mapper.list(!admin, term, (long) (safePage - 1) * safeSize, safeSize);
        return new Page(items, total, safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Item get(String identity, boolean admin) {
        Item item = admin ? mapper.byId(id(identity)) : mapper.publicBySlug(identity);
        if (item == null) notFound();
        return item;
    }

    @Override
    @Transactional
    public Item create(Request request) {
        validate(request);
        Item item = fromRequest(request);
        item.setSlug("new-" + UUID.randomUUID());
        mapper.insert(item);
        item.setSlug("listening-" + item.getId());
        mapper.update(item);
        mediaReferences.replace(item.getId(), item.getAudioMediaId());
        return get(String.valueOf(item.getId()), true);
    }

    @Override
    @Transactional
    public Item update(String identity, Request request) {
        validate(request);
        Item existing = get(identity, true);
        Item item = fromRequest(request);
        item.setId(existing.getId());
        item.setSlug(existing.getSlug());
        if (mapper.update(item) == 0) notFound();
        mediaReferences.replace(item.getId(), item.getAudioMediaId());
        return get(identity, true);
    }

    @Override
    @Transactional
    public Item setPublished(String identity, boolean published) {
        if (mapper.setStatus(id(identity), published ? "PUBLISHED" : "WITHDRAWN") == 0) notFound();
        return get(identity, true);
    }

    @Override
    @Transactional
    public void delete(String identity) {
        long id = id(identity);
        if (mapper.byId(id) == null) notFound();
        mediaReferences.remove(id);
        mapper.deleteSegments(id);
        mapper.delete(id);
    }

    private Item fromRequest(Request request) {
        Item item = new Item();
        item.setTitle(request.getTitle().trim());
        item.setSummary(request.getSummary() == null ? "" : request.getSummary().trim());
        item.setBodyMarkdown(request.getBodyMarkdown() == null ? "" : request.getBodyMarkdown());
        item.setCefrLevel(request.getCefrLevel() == null ? "A1" : request.getCefrLevel());
        item.setDifficultyLevel(request.getDifficultyLevel() == null ? 1 : request.getDifficultyLevel());
        item.setAudioMediaId(request.getAudioMediaId() == null || request.getAudioMediaId().isBlank()
                ? null : id(request.getAudioMediaId()));
        item.setDurationSeconds(request.getDurationSeconds() == null ? 0 : request.getDurationSeconds());
        item.setSourceName(request.getSourceName());
        item.setSourceUrl(request.getSourceUrl());
        item.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        return item;
    }

    private void validate(Request request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank())
            throw new ApiException("ENGLISH_DOCUMENT_INVALID", "请填写标题", 400);
        if (request.getTitle().length() > 200 || (request.getSummary() != null && request.getSummary().length() > 1000))
            throw new ApiException("ENGLISH_DOCUMENT_INVALID", "标题或摘要过长", 400);
        String level = request.getCefrLevel() == null ? "A1" : request.getCefrLevel();
        if (!List.of("A1", "A2", "B1", "B2", "C1", "C2").contains(level))
            throw new ApiException("ENGLISH_DOCUMENT_INVALID", "无效的 CEFR 等级", 400);
        if (request.getDifficultyLevel() != null && (request.getDifficultyLevel() < 1 || request.getDifficultyLevel() > 3))
            throw new ApiException("ENGLISH_DOCUMENT_INVALID", "难度只能是 1 至 3", 400);
        if (request.getDurationSeconds() != null && request.getDurationSeconds() < 0)
            throw new ApiException("ENGLISH_DOCUMENT_INVALID", "音频时长不能为负数", 400);
    }

    private void notFound() {
        throw new ApiException("ENGLISH_DOCUMENT_NOT_FOUND", "英语内容不存在", 404);
    }

    private long id(String value) {
        try { return Long.parseLong(value); }
        catch (NumberFormatException exception) {
            throw new ApiException("ENGLISH_ID_INVALID", "无效的内容编号", 400);
        }
    }
}
