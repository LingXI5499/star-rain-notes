package com.starrainnotes.review.service;

import com.starrainnotes.review.api.ReviewTargetKey;
import com.starrainnotes.review.api.dto.ReviewSubmissionCommand;
import com.starrainnotes.review.api.dto.ReviewSubmissionResult;
import com.starrainnotes.review.api.ReviewSummary;
import com.starrainnotes.review.api.ReviewSubmissionApi;
import java.util.Optional;

/*
 * REV-001 / REV-006 服务接口。
 *
 * 实现同时是 ReviewSubmissionApi：业务模块只看 api 包里的接口，
 * 不依赖这个 Service 包，这样 Review 内部重构不会牵动业务模块。
 */
public interface ReviewSubmissionService extends ReviewSubmissionApi {

    @Override
    ReviewSubmissionResult submit(ReviewSubmissionCommand command);

    @Override
    void cancelByApplicant(Long reviewRequestId, Long actorAccountId);

    @Override
    void cancelBySystem(ReviewTargetKey target, String reviewType, String reason);

    @Override
    Optional<ReviewSummary> findActivePending(ReviewTargetKey target, String reviewType);
}
