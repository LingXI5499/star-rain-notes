package com.starrainnotes.english.writing.service.impl;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.writing.dto.WritingPromptDto.Page;
import com.starrainnotes.english.writing.dto.WritingPromptDto.Prompt;
import com.starrainnotes.english.writing.dto.WritingPromptDto.Request;
import com.starrainnotes.english.writing.mapper.WritingPromptMapper;
import com.starrainnotes.english.writing.service.WritingPromptService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WritingPromptServiceImpl implements WritingPromptService {
    private final WritingPromptMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page list(boolean admin, String search, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(50, size));
        String term = search == null ? "" : search.trim();
        long total = mapper.count(!admin, term);
        List<Prompt> items = total == 0 ? List.of()
                : mapper.list(!admin, term, (long) (safePage - 1) * safeSize, safeSize);
        return new Page(items, total, safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Prompt get(String identity, boolean admin) {
        Prompt prompt = admin ? mapper.byId(id(identity)) : mapper.publicBySlug(identity);
        if (prompt == null) notFound();
        return prompt;
    }

    @Override
    @Transactional
    public Prompt create(Request request) {
        validate(request);
        Prompt prompt = fromRequest(request);
        prompt.setSlug("new-" + UUID.randomUUID());
        mapper.insert(prompt);
        prompt.setSlug("writing-prompts-" + prompt.getId());
        mapper.update(prompt);
        return get(String.valueOf(prompt.getId()), true);
    }

    @Override
    @Transactional
    public Prompt update(String identity, Request request) {
        validate(request);
        Prompt existing = get(identity, true);
        Prompt prompt = fromRequest(request);
        prompt.setId(existing.getId());
        prompt.setSlug(existing.getSlug());
        if (mapper.update(prompt) == 0) notFound();
        return get(identity, true);
    }

    @Override
    @Transactional
    public Prompt setPublished(String identity, boolean published) {
        if (mapper.setStatus(id(identity), published ? "PUBLISHED" : "WITHDRAWN") == 0) notFound();
        return get(identity, true);
    }

    @Override
    @Transactional
    public void delete(String identity) {
        if (mapper.delete(id(identity)) == 0) notFound();
    }

    private Prompt fromRequest(Request request) {
        Prompt prompt = new Prompt();
        prompt.setTitle(request.getTitle().trim());
        prompt.setSummary(request.getSummary() == null ? "" : request.getSummary().trim());
        prompt.setBodyMarkdown(request.getBodyMarkdown() == null ? "" : request.getBodyMarkdown());
        prompt.setRequirementsMarkdown(request.getRequirementsMarkdown() == null ? "" : request.getRequirementsMarkdown());
        prompt.setCefrLevel(request.getCefrLevel() == null ? "A1" : request.getCefrLevel());
        prompt.setWordMin(request.getWordMin());
        prompt.setWordMax(request.getWordMax());
        prompt.setEstimatedMinutes(request.getEstimatedMinutes());
        prompt.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        return prompt;
    }

    private void validate(Request request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank())
            throw new ApiException("ENGLISH_DOCUMENT_INVALID", "请填写标题", 400);
        if (request.getTitle().length() > 200 || (request.getSummary() != null && request.getSummary().length() > 1000))
            throw new ApiException("ENGLISH_DOCUMENT_INVALID", "标题或摘要过长", 400);
        String level = request.getCefrLevel() == null ? "A1" : request.getCefrLevel();
        if (!List.of("A1", "A2", "B1", "B2", "C1", "C2").contains(level))
            throw new ApiException("ENGLISH_DOCUMENT_INVALID", "无效的 CEFR 等级", 400);
        if (request.getWordMin() != null && request.getWordMax() != null && request.getWordMin() > request.getWordMax())
            throw new ApiException("ENGLISH_DOCUMENT_INVALID", "最少字数不能大于最多字数", 400);
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
