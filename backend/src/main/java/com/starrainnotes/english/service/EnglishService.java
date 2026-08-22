package com.starrainnotes.english.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.dto.EnglishView;
import com.starrainnotes.english.dto.UpdateEnglishRequest;
import com.starrainnotes.english.entity.EnglishOverview;
import com.starrainnotes.english.mapper.EnglishOverviewMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * English overview singleton management (04 §13). currentStage is never
 * changed by the admin request in V1 (stays FOUNDATION); there is no
 * word-memory backend in V1.
 */
@Service
public class EnglishService {

    private final EnglishOverviewMapper mapper;

    public EnglishService(EnglishOverviewMapper mapper) {
        this.mapper = mapper;
    }

    public EnglishView get() {
        return toView(requireSingleton());
    }

    public EnglishView update(UpdateEnglishRequest request) {
        EnglishOverview overview = requireSingleton();
        overview.setTitle(request.title());
        overview.setSubtitle(request.subtitle());
        overview.setIntroduction(request.introduction());
        overview.setRoadmapMarkdown(request.roadmapMarkdown());
        // currentStage is preserved: V1 stays FOUNDATION
        mapper.updateById(overview);
        return toView(overview);
    }

    private EnglishOverview requireSingleton() {
        EnglishOverview overview = mapper.selectById(1);
        if (overview == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_OVERVIEW_NOT_FOUND",
                    "English overview not found", "The english_overview singleton row is missing.");
        }
        return overview;
    }

    private EnglishView toView(EnglishOverview o) {
        return new EnglishView(o.getId(), o.getTitle(), o.getSubtitle(), o.getIntroduction(),
                o.getCurrentStage(), o.getRoadmapMarkdown());
    }
}
