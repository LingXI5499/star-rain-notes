package com.starrainnotes.review.dashboard;

import com.starrainnotes.common.dashboard.api.DashboardSource;
import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import com.starrainnotes.review.dto.ReviewQueryDTO;
import com.starrainnotes.review.service.ReviewQueryService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewDashboardSource implements DashboardSource {
    private final ReviewQueryService reviews;

    @Override public String moduleCode() { return "REVIEW"; }

    @Override public DashboardModuleData load() {
        ReviewQueryDTO query = new ReviewQueryDTO();
        query.setPageSize(1);
        return new DashboardModuleData(Map.of("pending", reviews.pagePending(query).getTotal()), List.of());
    }
}
