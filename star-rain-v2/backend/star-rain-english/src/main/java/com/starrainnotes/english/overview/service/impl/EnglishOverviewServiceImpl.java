package com.starrainnotes.english.overview.service.impl;

import com.starrainnotes.english.overview.mapper.EnglishOverviewMapper;

import com.starrainnotes.english.overview.entity.EnglishOverviewEntity;

import com.starrainnotes.english.overview.exception.EnglishOverviewNotFoundException;

import com.starrainnotes.english.overview.vo.EnglishOverviewVO;

import com.starrainnotes.english.overview.exception.EnglishOverviewInvalidException;

import com.starrainnotes.english.overview.dto.EnglishOverviewRequestDTO;

import com.starrainnotes.english.overview.service.EnglishOverviewService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnglishOverviewServiceImpl implements EnglishOverviewService {
    private final EnglishOverviewMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public EnglishOverviewVO get() {
        EnglishOverviewEntity entity = mapper.selectSingleton();
        if (entity == null) throw new EnglishOverviewNotFoundException();
        return EnglishOverviewVO.from(entity);
    }

    @Override
    @Transactional
    public EnglishOverviewVO update(EnglishOverviewRequestDTO request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()
                || request.getTitle().length() > 200)
            throw new EnglishOverviewInvalidException("标题需在 1 到 200 字之间");
        checkLength(request.getSubtitle(), 500, "副标题");
        checkLength(request.getIntroduction(), 10000, "介绍");
        checkLength(request.getRoadmapMarkdown(), 200000, "路线图");
        String title = request.getTitle().trim();
        String subtitle = blankToNull(request.getSubtitle());
        String introduction = blankToNull(request.getIntroduction());
        String roadmap = blankToNull(request.getRoadmapMarkdown());
        if (mapper.updateContent(title, subtitle, introduction, roadmap) != 1)
            throw new EnglishOverviewNotFoundException();
        return get();
    }

    private void checkLength(String value, int maxLength, String label) {
        if (value != null && value.length() > maxLength)
            throw new EnglishOverviewInvalidException(label + "过长");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
