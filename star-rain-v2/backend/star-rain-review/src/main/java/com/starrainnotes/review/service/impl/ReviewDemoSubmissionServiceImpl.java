package com.starrainnotes.review.service.impl;

import com.starrainnotes.review.api.dto.ReviewSubmissionCommand;
import com.starrainnotes.review.api.dto.ReviewSubmissionResult;
import com.starrainnotes.review.dto.ReviewDemoSubmissionDTO;
import com.starrainnotes.review.exception.ReviewTargetNotSupportedException;
import com.starrainnotes.review.handler.impl.ReviewDemoTargetHandler;
import com.starrainnotes.review.service.ReviewDemoSubmissionService;
import com.starrainnotes.review.service.ReviewSubmissionService;
import com.starrainnotes.review.utils.ReviewAccountLabels;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * 演示提交入口的实现。
 *
 * 它只做三件事：把浏览器入参收敛成 ReviewSubmissionCommand、
 * 用服务端的当前主体填 applicantAccountId、交给 ReviewSubmissionApi 提交。
 * 真正的业务校验（这个目标能不能提交、revisionRef 是多少）
 * 在 Tutorial 接入后由业务模块完成，本类随之删除。
 */
@Service
public class ReviewDemoSubmissionServiceImpl implements ReviewDemoSubmissionService {

    private final ReviewSubmissionService submissionService;

    public ReviewDemoSubmissionServiceImpl(ReviewSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @Override
    @Transactional
    public ReviewSubmissionResult submit(ReviewDemoSubmissionDTO request, Long applicantAccountId) {
        // 演示入口只放行演示目标：否则它就变成了「任意 target 都能提交」的后门
        if (!ReviewDemoTargetHandler.TARGET_MODULE.equals(request.getTargetModule())
                || !ReviewDemoTargetHandler.TARGET_TYPE.equals(request.getTargetType())
                || !ReviewDemoTargetHandler.REVIEW_TYPE.equals(request.getReviewType())) {
            throw new ReviewTargetNotSupportedException();
        }
        ReviewSubmissionCommand command = ReviewSubmissionCommand.builder()
                .reviewType(request.getReviewType())
                .targetModule(request.getTargetModule())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .targetRevisionRef(request.getTargetRevisionRef())
                .targetDisplayName(request.getTargetDisplayName())
                // 申请人来自认证上下文，浏览器无法伪造
                .applicantAccountId(applicantAccountId)
                .applicantDisplayName(ReviewAccountLabels.of(applicantAccountId))
                .submissionNote(request.getSubmissionNote())
                .build();
        return submissionService.submit(command);
    }
}
