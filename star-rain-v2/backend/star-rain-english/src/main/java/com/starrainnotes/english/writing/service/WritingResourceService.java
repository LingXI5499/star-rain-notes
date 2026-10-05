package com.starrainnotes.english.writing.service;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.writing.dto.WritingResourceDto.Page;
import com.starrainnotes.english.writing.dto.WritingResourceDto.Request;
import com.starrainnotes.english.writing.dto.WritingResourceDto.Resource;
import com.starrainnotes.english.writing.mapper.WritingResourceMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WritingResourceService {
    private final WritingResourceMapper mapper;

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

    @Transactional(readOnly = true)
    public Resource get(String identity, boolean admin) {
        Resource resource = admin ? mapper.byId(id(identity)) : mapper.publicBySlug(identity);
        if (resource == null) notFound();
        return resource;
    }

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

    @Transactional
    public Resource setPublished(String identity, boolean published) {
        if (mapper.setStatus(id(identity), published ? "PUBLISHED" : "WITHDRAWN") == 0) notFound();
        return get(identity, true);
    }

    @Transactional
    public void delete(String identity) {
        if (mapper.delete(id(identity)) == 0) notFound();
    }

    private Resource fromRequest(Request request) {
        Resource resource = new Resource();
        resource.setResourceKind(request.resourceKind() == null || request.resourceKind().isBlank()
                ? "RESOURCE" : request.resourceKind());
        resource.setTitle(request.title().trim());
        resource.setSummary(request.summary() == null ? "" : request.summary().trim());
        resource.setBodyMarkdown(request.bodyMarkdown() == null ? "" : request.bodyMarkdown());
        resource.setCefrLevel(request.cefrLevel() == null ? "A1" : request.cefrLevel());
        resource.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        return resource;
    }

    private void validate(Request request) {
        if (request == null || request.title() == null || request.title().isBlank())
            throw new ApiException("ENGLISH_DOCUMENT_INVALID", "请填写标题", 400);
        if (request.title().length() > 200 || (request.summary() != null && request.summary().length() > 1000))
            throw new ApiException("ENGLISH_DOCUMENT_INVALID", "标题或摘要过长", 400);
        String level = request.cefrLevel() == null ? "A1" : request.cefrLevel();
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
