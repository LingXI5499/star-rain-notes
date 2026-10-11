/*
 * ReviewQueryService：审核模块的业务契约文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：PageResult、ReviewViewer、ReviewHistoryQueryDTO、ReviewQueryDTO、ReviewDetailVO、ReviewHistoryVO、ReviewListItemVO。
 * 阅读入口：pagePending（分页读取待处理）；getDetail（读取详情）；pageHistory（分页读取历史）。
 */

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
/**
 * 审核查询参数业务服务，属于审核模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface ReviewQueryService {

    /**
     * 分页读取待处理。
     *
     * @param query 查询参数；类型为 <code>ReviewQueryDTO</code>
     * @return 类型为 <code>PageResult&lt;ReviewListItemVO&gt;</code> 的处理结果
     */
    PageResult<ReviewListItemVO> pagePending(ReviewQueryDTO query);

    /**
     * 读取详情。
     *
     * @param reviewRequestId 审核请求标识；类型为 <code>Long</code>
     * @param viewer viewer；类型为 <code>ReviewViewer</code>
     * @return 类型为 <code>ReviewDetailVO</code> 的处理结果
     */
    ReviewDetailVO getDetail(Long reviewRequestId, ReviewViewer viewer);

    /**
     * 分页读取历史。
     *
     * @param query 查询参数；类型为 <code>ReviewHistoryQueryDTO</code>
     * @return 类型为 <code>PageResult&lt;ReviewHistoryVO&gt;</code> 的处理结果
     */
    PageResult<ReviewHistoryVO> pageHistory(ReviewHistoryQueryDTO query);
}
