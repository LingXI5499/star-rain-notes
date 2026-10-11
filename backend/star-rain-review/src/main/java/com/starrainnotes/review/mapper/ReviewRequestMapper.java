package com.starrainnotes.review.mapper;

import com.starrainnotes.review.entity.ReviewRequestEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 审核请求数据访问。
 *
 * 三条约定：
 *   1. 任何状态变更都必须走 *IfPending 条件更新，affected_rows 必须为 1；
 *      绝不允许「先 select 状态 → if pending → 普通 update」的写法，那有竞态。
 *   2. 时间一律由 Service 传入 LocalDateTime，不用数据库 NOW()，
 *      这样单测可以不依赖数据库时钟。
 *   3. 不改目标业务模块的表，任何业务状态都由 TargetHandler 回调推进。
 */
@Mapper
public interface ReviewRequestMapper {

    int insertRequest(ReviewRequestEntity request);

    ReviewRequestEntity requestById(@Param("id") Long id);

    // 同一 target + reviewType 的待审请求；不存在返回 null
    ReviewRequestEntity selectPending(@Param("targetModule") String targetModule,
                                      @Param("targetType") String targetType,
                                      @Param("targetId") Long targetId,
                                      @Param("reviewType") String reviewType);

    long pendingCount(@Param("targetModule") String targetModule,
                      @Param("reviewType") String reviewType,
                      @Param("keyword") String keyword);

    List<ReviewRequestEntity> pendingPage(@Param("targetModule") String targetModule,
                                          @Param("reviewType") String reviewType,
                                          @Param("keyword") String keyword,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    long historyCount(@Param("targetModule") String targetModule,
                      @Param("targetType") String targetType,
                      @Param("targetId") Long targetId,
                      @Param("reviewType") String reviewType,
                      @Param("status") String status,
                      @Param("applicantAccountId") Long applicantAccountId,
                      @Param("reviewerAccountId") Long reviewerAccountId,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    List<ReviewRequestEntity> historyPage(@Param("targetModule") String targetModule,
                                          @Param("targetType") String targetType,
                                          @Param("targetId") Long targetId,
                                          @Param("reviewType") String reviewType,
                                          @Param("status") String status,
                                          @Param("applicantAccountId") Long applicantAccountId,
                                          @Param("reviewerAccountId") Long reviewerAccountId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    // 以下三个条件更新的返回值就是「本次决定是否生效」的唯一判据
    int approveIfPending(@Param("id") Long id,
                         @Param("reviewerAccountId") Long reviewerAccountId,
                         @Param("reason") String reason,
                         @Param("decidedAt") LocalDateTime decidedAt);

    int rejectIfPending(@Param("id") Long id,
                        @Param("reviewerAccountId") Long reviewerAccountId,
                        @Param("reason") String reason,
                        @Param("decidedAt") LocalDateTime decidedAt);

    int cancelIfPending(@Param("id") Long id,
                        @Param("canceledAt") LocalDateTime canceledAt);

    /*
     * 按目标维度取一个 MySQL 命名锁，给「同一目标只能一个 PENDING」的检查加串行化。
     *
     * 普通唯一索引无法只对 PENDING 生效，而 SELECT ... FOR UPDATE 在行还不存在时
     * 拦不住两个并发插入（间隙锁在不同索引路径下不可靠）。命名锁把检查 + 插入
     * 变成一个临界区，代价只是同一目标维度的提交被短暂串行。
     * 返回 1 表示拿到锁，0 表示超时。
     */
    int acquireTargetLock(@Param("lockName") String lockName, @Param("timeoutSeconds") int timeoutSeconds);

    int releaseTargetLock(@Param("lockName") String lockName);
}
