package com.starrainnotes.review.mapper;

import com.starrainnotes.review.entity.ReviewActionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 审核动作历史数据访问。
 *
 * 只插入与查询，没有 update / delete：历史一旦写下就是事实，
 * 允许修改历史等于允许篡改审核轨迹。
 */
@Mapper
public interface ReviewActionMapper {

    int insertAction(ReviewActionEntity action);

    List<ReviewActionEntity> selectByReviewRequestId(@Param("reviewRequestId") Long reviewRequestId);
}
