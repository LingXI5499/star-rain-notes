/*
 * ReviewRequestMapper：审核模块的数据访问文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：ReviewRequestEntity。
 * 阅读入口：insertRequest（写入请求）；requestById（按标识读取本次操作的请求参数对象）；selectPending（查询待处理处理入口）；pendingCount（待处理数量处理入口）。
 */

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
/**
 * 审核请求数据映射，属于审核模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface ReviewRequestMapper {

    /**
     * 写入请求。
     *
     * 持久化操作：新增，涉及 <code>sr_review_request</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ReviewRequestEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insertRequest(ReviewRequestEntity request);

    /**
     * 按标识读取本次操作的请求参数对象。
     *
     * 持久化操作：读取，涉及 <code>sr_review_request</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>ReviewRequestEntity</code> 的处理结果
     */
    ReviewRequestEntity requestById(@Param("id") Long id);

    // 同一 target + reviewType 的待审请求；不存在返回 null
    /**
     * 查询待处理处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_review_request</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param targetModule 目标对象所属模块；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("targetModule")</code>
     * @param targetType 目标对象的业务类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("targetType")</code>
     * @param targetId 被处理或审核的目标对象标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("targetId")</code>
     * @param reviewType 审核类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("reviewType")</code>
     * @return 类型为 <code>ReviewRequestEntity</code> 的处理结果
     */
    ReviewRequestEntity selectPending(@Param("targetModule") String targetModule,
                                      @Param("targetType") String targetType,
                                      @Param("targetId") Long targetId,
                                      @Param("reviewType") String reviewType);

    /**
     * 待处理数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_review_request</code>。
     *
     * @param targetModule 目标对象所属模块；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("targetModule")</code>
     * @param reviewType 审核类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("reviewType")</code>
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long pendingCount(@Param("targetModule") String targetModule,
                      @Param("reviewType") String reviewType,
                      @Param("keyword") String keyword);

    /**
     * 待处理分页处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_review_request</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param targetModule 目标对象所属模块；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("targetModule")</code>
     * @param reviewType 审核类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("reviewType")</code>
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @param offset 查询跳过的记录数；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<ReviewRequestEntity> pendingPage(@Param("targetModule") String targetModule,
                                          @Param("reviewType") String reviewType,
                                          @Param("keyword") String keyword,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    /**
     * 历史数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_review_request</code>。
     *
     * @param targetModule 目标对象所属模块；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("targetModule")</code>
     * @param targetType 目标对象的业务类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("targetType")</code>
     * @param targetId 被处理或审核的目标对象标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("targetId")</code>
     * @param reviewType 审核类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("reviewType")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param applicantAccountId 申请人账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("applicantAccountId")</code>
     * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("reviewerAccountId")</code>
     * @param startTime 开始时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("startTime")</code>
     * @param endTime 结束时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("endTime")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long historyCount(@Param("targetModule") String targetModule,
                      @Param("targetType") String targetType,
                      @Param("targetId") Long targetId,
                      @Param("reviewType") String reviewType,
                      @Param("status") String status,
                      @Param("applicantAccountId") Long applicantAccountId,
                      @Param("reviewerAccountId") Long reviewerAccountId,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    /**
     * 历史分页处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_review_request</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param targetModule 目标对象所属模块；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("targetModule")</code>
     * @param targetType 目标对象的业务类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("targetType")</code>
     * @param targetId 被处理或审核的目标对象标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("targetId")</code>
     * @param reviewType 审核类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("reviewType")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param applicantAccountId 申请人账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("applicantAccountId")</code>
     * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("reviewerAccountId")</code>
     * @param startTime 开始时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("startTime")</code>
     * @param endTime 结束时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("endTime")</code>
     * @param offset 查询跳过的记录数；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
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
    /**
     * 批准If待处理。
     *
     * 持久化操作：更新，涉及 <code>sr_review_request</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("reviewerAccountId")</code>
     * @param reason 原因；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("reason")</code>
     * @param decidedAt 决定时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("decidedAt")</code>
     * @return SQL 写入影响的记录行数
     */
    int approveIfPending(@Param("id") Long id,
                         @Param("reviewerAccountId") Long reviewerAccountId,
                         @Param("reason") String reason,
                         @Param("decidedAt") LocalDateTime decidedAt);

    /**
     * 拒绝If待处理。
     *
     * 持久化操作：更新，涉及 <code>sr_review_request</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("reviewerAccountId")</code>
     * @param reason 原因；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("reason")</code>
     * @param decidedAt 决定时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("decidedAt")</code>
     * @return SQL 写入影响的记录行数
     */
    int rejectIfPending(@Param("id") Long id,
                        @Param("reviewerAccountId") Long reviewerAccountId,
                        @Param("reason") String reason,
                        @Param("decidedAt") LocalDateTime decidedAt);

    /**
     * 取消If待处理。
     *
     * 持久化操作：更新，涉及 <code>sr_review_request</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param canceledAt canceled时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("canceledAt")</code>
     * @return SQL 写入影响的记录行数
     */
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
    /**
     * 获取目标锁。
     *
     * 持久化语句类型：<code>select</code>。
     *
     * @param lockName 锁名称；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("lockName")</code>
     * @param timeoutSeconds 超时秒；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("timeoutSeconds")</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    int acquireTargetLock(@Param("lockName") String lockName, @Param("timeoutSeconds") int timeoutSeconds);

    /**
     * 释放目标锁。
     *
     * 持久化语句类型：<code>select</code>。
     *
     * @param lockName 锁名称；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("lockName")</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    int releaseTargetLock(@Param("lockName") String lockName);
}
