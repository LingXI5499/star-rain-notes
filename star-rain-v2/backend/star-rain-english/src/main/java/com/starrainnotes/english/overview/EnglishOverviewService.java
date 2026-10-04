package com.starrainnotes.english.overview;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnglishOverviewService {
    private final EnglishOverviewMapper mapper;

    @Transactional(readOnly = true)
    public EnglishOverviewView get() {
        EnglishOverviewEntity entity = mapper.selectById(1);
        if (entity == null) throw new EnglishOverviewNotFoundException();
        return EnglishOverviewView.from(entity);
    }

    @Transactional
    public EnglishOverviewView update(EnglishOverviewRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()
                || request.title().length() > 200)
            throw new EnglishOverviewInvalidException("标题需在 1 到 200 字之间");
        checkLength(request.subtitle(), 500, "副标题");
        checkLength(request.introduction(), 10000, "介绍");
        checkLength(request.roadmapMarkdown(), 200000, "路线图");
        String title = request.title().trim();
        String subtitle = blankToNull(request.subtitle());
        String introduction = blankToNull(request.introduction());
        String roadmap = blankToNull(request.roadmapMarkdown());
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
