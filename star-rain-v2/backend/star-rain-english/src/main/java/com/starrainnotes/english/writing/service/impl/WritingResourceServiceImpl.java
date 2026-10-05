package com.starrainnotes.english.writing.service.impl;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.writing.dto.WritingResourceDto.Page;
import com.starrainnotes.english.writing.dto.WritingResourceDto.Request;
import com.starrainnotes.english.writing.dto.WritingResourceDto.Resource;
import com.starrainnotes.english.writing.mapper.WritingResourceMapper;
import com.starrainnotes.english.writing.service.WritingResourceService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WritingResourceServiceImpl implements WritingResourceService {
    private final WritingResourceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page list(boolean admin, String search, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(50, size));
        String term = search == null ? "" : search.trim();
        long total = mapper.count(!admin, term);
        List<Resource> items = total == 0 ? List.of()
                : mapper.list(!admin, term, (long) (safePage - 1) * safeSize, safeSize);
        return new Page(items, total, safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource get(String identity, boolean admin) {
        Resource resource = admin ? mapper.byId(id(identity)) : mapper.publicBySlug(identity);
        if (resource == null) notFound();
        return resource;
    }

    @Override
    @Transactional
    public Resource create(Request request) {
        validate(request);
        Resource resource = fromRequest(request);
        resource.setSlug("new-" + UUID.randomUUID());
        mapper.insert(resource);
        resource.setSlug("writing-resources-" + resource.getId());
        mapper.update(resource);
        return get(String.valueOf(resource.getId()), true);
    }

    @Override
    @Transactional
    public Resource update(String identity, Request request) {
        validate(request);
        Resource existing = get(identity, true);
        Resource resource = fromRequest(request);
        resource.setId(existing.getId());
        resource.setSlug(existing.getSlug());
        if (mapper.update(resource) == 0) notFound();
        return get(identity, true);
    }

    @Override
    @Transactional
    public Resource setPublished(String identity, boolean published) {
        if (mapper.setStatus(id(identity), published ? "PUBLISHED" : "WITHDRAWN") == 0) notFound();
        return get(identity, true);
    }

    @Override
    @Transactional
    public void delete(String identity) {
        if (mapper.delete(id(identity)) == 0) notFound();
    }

    private Resource fromRequest(Request request) {
        Resource resource = new Resource();
        resource.setResourceKind(request.getResourceKind() == null || request.getResourceKind().isBlank()
                ? "RESOURCE" : request.getResourceKind());
        resource.setTitle(request.getTitle().trim());
        resource.setSummary(request.getSummary() == null ? "" : request.getSummary().trim());
        resource.setBodyMarkdown(request.getBodyMarkdown() == null ? "" : request.getBodyMarkdown());
        resource.setCefrLevel(request.getCefrLevel() == null ? "A1" : request.getCefrLevel());
        resource.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        return resource;
    }

    private void validate(Request request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank())
            throw new ApiException("ENGLISH_DOCUMENT_INVALID", "请填写标题", 400);
        if (request.getTitle().length() > 200 || (request.getSummary() != null && request.getSummary().length() > 1000))
            throw new ApiException("ENGLISH_DOCUMENT_INVALID", "标题或摘要过长", 400);
        String level = request.getCefrLevel() == null ? "A1" : request.getCefrLevel();
        if (!List.of("A1", "A2", "B1", "B2", "C1", "C2").contains(level))
            throw new ApiException("ENGLISH_DOCUMENT_INVALID", "无效的 CEFR 等级", 400);
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
