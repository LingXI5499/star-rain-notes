/*
 * ReviewSubmissionService：审核模块的业务契约文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：ReviewTargetKey、ReviewSubmissionCommand、ReviewSubmissionResult、ReviewSummary、ReviewSubmissionApi。
 * 阅读入口：submit（提交审核Submission）；cancelByApplicant（取消依据申请人）；cancelBySystem（取消依据System）；findActivePending（查找有效待处理）。
 */

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
/**
 * 审核Submission业务服务，属于审核模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface ReviewSubmissionService extends ReviewSubmissionApi {

    /**
     * 提交审核Submission。
     *
     * @param command command；类型为 <code>ReviewSubmissionCommand</code>
     * @return 类型为 <code>ReviewSubmissionResult</code> 的处理结果
     */
    @Override
    ReviewSubmissionResult submit(ReviewSubmissionCommand command);

    /**
     * 取消依据申请人。
     *
     * @param reviewRequestId 审核请求标识；类型为 <code>Long</code>
     * @param actorAccountId 操作者账号标识；类型为 <code>Long</code>
     */
    @Override
    void cancelByApplicant(Long reviewRequestId, Long actorAccountId);

    /**
     * 取消依据System。
     *
     * @param target 目标；类型为 <code>ReviewTargetKey</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @param reason 原因；类型为 <code>String</code>
     */
    @Override
    void cancelBySystem(ReviewTargetKey target, String reviewType, String reason);

    /**
     * 查找有效待处理。
     *
     * @param target 目标；类型为 <code>ReviewTargetKey</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    Optional<ReviewSummary> findActivePending(ReviewTargetKey target, String reviewType);
}
