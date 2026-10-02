package com.starrainnotes.review.service;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.review.context.ReviewViewer;
import com.starrainnotes.review.dto.ReviewHistoryQueryDTO;
import com.starrainnotes.review.dto.ReviewQueryDTO;
import com.starrainnotes.review.vo.ReviewDetailVO;
import com.starrainnotes.review.vo.ReviewHistoryVO;
import com.starrainnotes.review.vo.ReviewListItemVO;

/*
 * REV-002 / REV-003 / REV-007 服务接口。
 *
 * getDetail 必须传 ReviewViewer：REV-003 允许申请人看自己的申请，
 * 因此对象级授权是详情查询的一部分，不能只靠 URL 层的 authenticated()。
 */
public interface ReviewQueryService {

    PageResult<ReviewListItemVO> pagePending(ReviewQueryDTO query);

    ReviewDetailVO getDetail(Long reviewRequestId, ReviewViewer viewer);

    PageResult<ReviewHistoryVO> pageHistory(ReviewHistoryQueryDTO query);
}
