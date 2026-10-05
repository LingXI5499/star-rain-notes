package com.starrainnotes.english.reading.service.impl;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.reading.dto.ReadingDto.Article;
import com.starrainnotes.english.reading.dto.ReadingDto.Page;
import com.starrainnotes.english.reading.dto.ReadingDto.Request;
import com.starrainnotes.english.reading.mapper.ReadingMapper;
import com.starrainnotes.english.reading.service.ReadingService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReadingServiceImpl implements ReadingService {
    private final ReadingMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page list(boolean admin, String search, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(50, size));
        String term = search == null ? "" : search.trim();
        long total = mapper.count(!admin, term);
        List<Article> items = total == 0 ? List.of()
                : mapper.list(!admin, term, (long) (safePage - 1) * safeSize, safeSize);
        return new Page(items, total, safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Article get(String identity, boolean admin) {
        Article article = admin ? mapper.byId(id(identity)) : mapper.publicBySlug(identity);
        if (article == null) notFound();
        return article;
    }

    @Override
    @Transactional
    public Article create(Request request) {
        validate(request);
        Article article = fromRequest(request);
        article.setSlug("new-" + UUID.randomUUID());
        mapper.insert(article);
        article.setSlug("reading-" + article.getId());
        mapper.update(article);
        return get(String.valueOf(article.getId()), true);
    }

    @Override
    @Transactional
    public Article update(String identity, Request request) {
        validate(request);
        Article existing = get(identity, true);
        Article article = fromRequest(request);
        article.setId(existing.getId());
        article.setSlug(existing.getSlug());
        if (mapper.update(article) == 0) notFound();
        return get(identity, true);
    }

    @Override
    @Transactional
    public Article setPublished(String identity, boolean published) {
        if (mapper.setStatus(id(identity), published ? "PUBLISHED" : "WITHDRAWN") == 0) notFound();
        return get(identity, true);
    }

    @Override
    @Transactional
    public void delete(String identity) {
        if (mapper.delete(id(identity)) == 0) notFound();
    }

    private Article fromRequest(Request request) {
        Article article = new Article();
        article.setTitle(request.getTitle().trim());
        article.setSummary(request.getSummary() == null ? "" : request.getSummary().trim());
        article.setBodyMarkdown(request.getBodyMarkdown() == null ? "" : request.getBodyMarkdown());
        article.setCefrLevel(request.getCefrLevel() == null ? "A1" : request.getCefrLevel());
        article.setDifficultyLevel(request.getDifficultyLevel() == null ? 1 : request.getDifficultyLevel());
        article.setSourceName(request.getSourceName());
        article.setSourceUrl(request.getSourceUrl());
        article.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        return article;
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
