/*
 * ReviewQueryServiceImplTest：审核模块的测试文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：ApiException、PageResult、ReviewPermissions、ReviewViewer、ReviewHistoryQueryDTO、ReviewQueryDTO、ReviewActionEntity。
 * 阅读入口：service（业务服务处理入口）；request（本次操作的请求参数对象处理入口）；viewer（viewer处理入口）；action（动作处理入口）。
 */

package com.starrainnotes.review.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.review.constant.ReviewPermissions;
import com.starrainnotes.review.context.ReviewViewer;
import com.starrainnotes.review.dto.ReviewHistoryQueryDTO;
import com.starrainnotes.review.dto.ReviewQueryDTO;
import com.starrainnotes.review.entity.ReviewActionEntity;
import com.starrainnotes.review.entity.ReviewRequestEntity;
import com.starrainnotes.review.enumeration.ReviewActionType;
import com.starrainnotes.review.enumeration.ReviewStatus;
import com.starrainnotes.review.handler.impl.ReviewDemoTargetHandler;
import com.starrainnotes.review.handler.impl.ReviewTargetHandlerRegistryImpl;
import com.starrainnotes.review.mapper.ReviewActionMapper;
import com.starrainnotes.review.mapper.ReviewRequestMapper;
import com.starrainnotes.review.vo.ReviewActionVO;
import com.starrainnotes.review.vo.ReviewDetailVO;
import com.starrainnotes.review.vo.ReviewHistoryVO;
import com.starrainnotes.review.vo.ReviewListItemVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/*
 * REV-002 待审核列表 / REV-003 审核详情 / REV-007 审核历史的单元测试。
 *
 * 三类断言：
 *   1. 查询参数非法必须明确报错，不静默纠正；
 *   2. 详情的对象级授权：申请人本人可看，无关账户被拒（IDOR 防护）；
 *   3. 详情必须按 revisionRef 取目标视图，版本读不到就失败，绝不返回空详情让人误审。
 */
/**
 * 审核查询参数业务服务实现测试，属于审核模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewQueryServiceImplTest {

    /**
     * 请求数据映射。
     * 协作类型：<code>ReviewRequestMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private ReviewRequestMapper requestMapper;

    /**
     * 动作数据映射。
     * 协作类型：<code>ReviewActionMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private ReviewActionMapper actionMapper;

    /**
     * 业务服务处理入口。
     * 关键处理步骤调用 <code>List.of</code>。
     * @return 类型为 <code>ReviewQueryServiceImpl</code> 的处理结果
     */
    private ReviewQueryServiceImpl service() {
        return new ReviewQueryServiceImpl(requestMapper, actionMapper,
                new ReviewTargetHandlerRegistryImpl(List.of(new ReviewDemoTargetHandler())));
    }

    /**
     * 本次操作的请求参数对象处理入口。
     * 关键处理步骤调用 <code>entity.setId</code>、<code>entity.setReviewType</code>、<code>entity.setTargetModule</code>、<code>entity.setTargetType</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @return 类型为 <code>ReviewRequestEntity</code> 的处理结果
     */
    private static ReviewRequestEntity request(Long id, String status) {
        ReviewRequestEntity entity = new ReviewRequestEntity();
        entity.setId(id);
        entity.setReviewType(ReviewDemoTargetHandler.REVIEW_TYPE);
        entity.setTargetModule(ReviewDemoTargetHandler.TARGET_MODULE);
        entity.setTargetType(ReviewDemoTargetHandler.TARGET_TYPE);
        entity.setTargetId(100L);
        entity.setTargetRevisionRef("revision:7");
        entity.setTargetDisplayName("《Java 程序设计》");
        entity.setApplicantAccountId(9L);
        entity.setApplicantDisplayName("账户 #9");
        entity.setStatus(status);
        entity.setSubmittedAt(LocalDateTime.now());
        return entity;
    }

    /**
     * viewer处理入口。
     * 关键处理步骤调用 <code>ReviewViewer.builder</code>、<code>Set.of</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @param permissions 权限集合；类型为 <code>String[]</code>
     * @return 类型为 <code>ReviewViewer</code> 的处理结果
     */
    private static ReviewViewer viewer(Long accountId, String... permissions) {
        return ReviewViewer.builder().accountId(accountId).permissions(Set.of(permissions)).build();
    }

    /**
     * 动作处理入口。
     * 关键处理步骤调用 <code>action.setId</code>、<code>action.setReviewRequestId</code>、<code>action.setActionType</code>、<code>action.setActorAccountId</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param type 类型；类型为 <code>String</code>
     * @param actorAccountId 操作者账号标识；类型为 <code>Long</code>
     * @return 类型为 <code>ReviewActionEntity</code> 的处理结果
     */
    private static ReviewActionEntity action(Long id, String type, Long actorAccountId) {
        ReviewActionEntity action = new ReviewActionEntity();
        action.setId(id);
        action.setReviewRequestId(500L);
        action.setActionType(type);
        action.setActorAccountId(actorAccountId);
        action.setActorType("ACCOUNT");
        action.setNote("备注");
        action.setCreatedAt(LocalDateTime.now());
        return action;
    }

    // ---------- REV-002 ----------

    /**
     * \u5f85\u5ba1\u6838\u5217\u8868\uff1a\u53ea\u67e5 PENDING\uff0c\u8fd4\u56de\u5206\u9875\u7ed3\u679c\u4e0e\u5c55\u793a\u5feb\u7167。
     * 主要协作调用：<code>requestMapper.pendingCount</code>、<code>requestMapper.pendingPage</code>。
     */
    @Test
    @DisplayName("待审核列表：只查 PENDING，返回分页结果与展示快照")
    void pendingPageReturnsListItems() {
        ReviewQueryDTO query = new ReviewQueryDTO();
        when(requestMapper.pendingCount(any(), any(), any())).thenReturn(1L);
        when(requestMapper.pendingPage(any(), any(), any(), eq(0), eq(20)))
                .thenReturn(List.of(request(500L, ReviewStatus.PENDING_CODE)));

        PageResult<ReviewListItemVO> result = service().pagePending(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(1);
        ReviewListItemVO item = result.getItems().get(0);
        assertThat(item.getReviewId()).isEqualTo(500L);
        assertThat(item.getTargetDisplayName()).isEqualTo("《Java 程序设计》");
        assertThat(item.getApplicantDisplayName()).isEqualTo("账户 #9");
        // 列表行刻意不含 targetRevisionRef：待办只需要展示快照，版本引用是详情才需要的字段
        assertThat(ReviewListItemVO.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("targetRevisionRef");
    }

    /**
     * pageSize \u8d8a\u754c\u8fd4\u56de REVIEW_QUERY_INVALID\uff0c\u800c\u4e0d\u662f\u9759\u9ed8\u7ea0\u6b63\u6210\u9ed8\u8ba4\u503c。
     * 关键处理步骤调用 <code>query.setPageSize</code>、<code>service</code>。
     */
    @Test
    @DisplayName("pageSize 越界返回 REVIEW_QUERY_INVALID，而不是静默纠正成默认值")
    void pendingRejectsInvalidPageSize() {
        ReviewQueryDTO query = new ReviewQueryDTO();
        query.setPageSize(500);

        assertThatThrownBy(() -> service().pagePending(query))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_QUERY_INVALID");
    }

    /**
     * page \u5c0f\u4e8e 1 \u8fd4\u56de REVIEW_QUERY_INVALID。
     * 关键处理步骤调用 <code>query.setPage</code>、<code>service</code>。
     */
    @Test
    @DisplayName("page 小于 1 返回 REVIEW_QUERY_INVALID")
    void pendingRejectsInvalidPage() {
        ReviewQueryDTO query = new ReviewQueryDTO();
        query.setPage(0);

        assertThatThrownBy(() -> service().pagePending(query))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_QUERY_INVALID");
    }

    /**
     * \u7b5b\u9009\u6761\u4ef6\u7a7a\u767d\u89c6\u4e3a\u4e0d\u7b5b\u9009\uff0c\u524d\u540e\u7a7a\u683c\u88ab\u88c1\u526a。
     * 主要协作调用：<code>requestMapper.pendingCount</code>、<code>requestMapper.pendingPage</code>。
     */
    @Test
    @DisplayName("筛选条件空白视为不筛选，前后空格被裁剪")
    void pendingNormalizesFilters() {
        ReviewQueryDTO query = new ReviewQueryDTO();
        query.setTargetModule("  ");
        query.setReviewType("  ");
        query.setKeyword("  Java  ");
        when(requestMapper.pendingCount(isNull(), isNull(), eq("Java"))).thenReturn(0L);
        when(requestMapper.pendingPage(isNull(), isNull(), eq("Java"), eq(0), eq(20)))
                .thenReturn(List.of());

        PageResult<ReviewListItemVO> result = service().pagePending(query);

        // stub 匹配 isNull()/eq("Java") 即说明空白与前后空格都被归一化，否则会返回 null 而 NPE
        assertThat(result.getTotal()).isZero();
        assertThat(result.getItems()).isEmpty();
    }

    // ---------- REV-003 ----------

    /**
     * \u8be6\u60c5\uff1a\u7533\u8bf7\u4eba\u672c\u4eba\u53ef\u4ee5\u67e5\u770b\u81ea\u5df1\u7684\u7533\u8bf7\uff0c\u4e14\u4e0d\u80fd\u6267\u884c\u5ba1\u6279\u52a8\u4f5c。
     * 主要协作调用：<code>requestMapper.requestById</code>、<code>actionMapper.selectByReviewRequestId</code>。
     */
    @Test
    @DisplayName("详情：申请人本人可以查看自己的申请，且不能执行审批动作")
    void detailAllowedForApplicant() {
        when(requestMapper.requestById(500L)).thenReturn(request(500L, ReviewStatus.PENDING_CODE));
        when(actionMapper.selectByReviewRequestId(500L))
                .thenReturn(List.of(action(1L, ReviewActionType.SUBMITTED_CODE, 9L)));

        ReviewDetailVO detail = service().getDetail(500L, viewer(9L));

        assertThat(detail.getReviewId()).isEqualTo(500L);
        assertThat(detail.getTargetView()).isNotNull();
        assertThat(detail.getTargetView().viewType()).isEqualTo("DEMO_TARGET_SNAPSHOT");
        assertThat(detail.getHistory()).hasSize(1);
        // 申请人没有 review:approve / review:reject，按钮必须关掉
        assertThat(detail.isCanApprove()).isFalse();
        assertThat(detail.isCanReject()).isFalse();
        assertThat(detail.isCanCancel()).isTrue();
    }

    /**
     * \u8be6\u60c5\uff1a\u65e0\u5173\u8d26\u6237\u67e5\u770b\u88ab\u62d2\u4e3a REVIEW_ACCESS_DENIED\uff08IDOR \u9632\u62a4\uff09。
     * 主要协作调用：<code>requestMapper.requestById</code>。
     */
    @Test
    @DisplayName("详情：无关账户查看被拒为 REVIEW_ACCESS_DENIED（IDOR 防护）")
    void detailDeniedForUnrelatedAccount() {
        when(requestMapper.requestById(500L)).thenReturn(request(500L, ReviewStatus.PENDING_CODE));

        assertThatThrownBy(() -> service().getDetail(500L, viewer(77L)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_ACCESS_DENIED");
    }

    /**
     * \u8be6\u60c5\uff1a\u62e5\u6709 review:read \u7684 Reviewer \u53ef\u4ee5\u67e5\u770b\u5e76\u53ef\u4ee5\u5ba1\u6279。
     * 主要协作调用：<code>requestMapper.requestById</code>、<code>actionMapper.selectByReviewRequestId</code>。
     */
    @Test
    @DisplayName("详情：拥有 review:read 的 Reviewer 可以查看并可以审批")
    void detailAllowedForReviewer() {
        when(requestMapper.requestById(500L)).thenReturn(request(500L, ReviewStatus.PENDING_CODE));
        when(actionMapper.selectByReviewRequestId(500L)).thenReturn(List.of());

        ReviewDetailVO detail = service().getDetail(500L,
                viewer(1L, ReviewPermissions.READ, ReviewPermissions.APPROVE, ReviewPermissions.REJECT));

        assertThat(detail.isCanApprove()).isTrue();
        assertThat(detail.isCanReject()).isTrue();
        // 不是申请人，因此不能替申请人取消
        assertThat(detail.isCanCancel()).isFalse();
    }

    /**
     * \u8be6\u60c5\uff1a\u7ec8\u6001\u8bf7\u6c42\u4e0d\u518d\u63d0\u4f9b\u4efb\u4f55\u53ef\u6267\u884c\u52a8\u4f5c。
     * 主要协作调用：<code>requestMapper.requestById</code>、<code>actionMapper.selectByReviewRequestId</code>。
     */
    @Test
    @DisplayName("详情：终态请求不再提供任何可执行动作")
    void detailHidesActionsForTerminalStatus() {
        when(requestMapper.requestById(500L)).thenReturn(request(500L, ReviewStatus.APPROVED_CODE));
        when(actionMapper.selectByReviewRequestId(500L)).thenReturn(List.of());

        ReviewDetailVO detail = service().getDetail(500L,
                viewer(9L, ReviewPermissions.READ, ReviewPermissions.APPROVE, ReviewPermissions.REJECT));

        assertThat(detail.isCanApprove()).isFalse();
        assertThat(detail.isCanReject()).isFalse();
        assertThat(detail.isCanCancel()).isFalse();
    }

    /**
     * \u8be6\u60c5\uff1a\u51bb\u7ed3\u7248\u672c\u8bfb\u4e0d\u5230\u65f6\u62a5 REVIEW_TARGET_REVISION_NOT_FOUND\uff0c\u7edd\u4e0d\u8fd4\u56de\u7a7a\u8be6\u60c5\u8ba9\u4eba\u8bef\u5ba1。
     * 主要协作调用：<code>requestMapper.requestById</code>。
     */
    @Test
    @DisplayName("详情：冻结版本读不到时报 REVIEW_TARGET_REVISION_NOT_FOUND，绝不返回空详情让人误审")
    void detailFailsWhenFrozenRevisionUnreadable() {
        ReviewRequestEntity entity = request(500L, ReviewStatus.PENDING_CODE);
        // 模拟「提交时的那个版本已经读不到」
        entity.setTargetRevisionRef("   ");
        when(requestMapper.requestById(500L)).thenReturn(entity);

        assertThatThrownBy(() -> service().getDetail(500L, viewer(1L, ReviewPermissions.READ)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_TARGET_REVISION_NOT_FOUND");
    }

    /**
     * \u8be6\u60c5\uff1a\u5ba1\u6838\u8bf7\u6c42\u4e0d\u5b58\u5728\u8fd4\u56de REVIEW_NOT_FOUND。
     * 主要协作调用：<code>requestMapper.requestById</code>。
     */
    @Test
    @DisplayName("详情：审核请求不存在返回 REVIEW_NOT_FOUND")
    void detailUnknownRequest() {
        when(requestMapper.requestById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service().getDetail(404L, viewer(1L, ReviewPermissions.READ)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_NOT_FOUND");
    }

    /**
     * \u52a8\u4f5c\u5386\u53f2\uff1a\u7533\u8bf7\u4eba\u7684\u52a8\u4f5c\u5e26\u663e\u793a\u540d\uff0c\u51b3\u7b56\u7c7b\u52a8\u4f5c\u4e0d\u7f16\u9020\u540d\u5b57。
     * 主要协作调用：<code>requestMapper.requestById</code>、<code>actionMapper.selectByReviewRequestId</code>。
     */
    @Test
    @DisplayName("动作历史：申请人的动作带显示名，决策类动作不编造名字")
    void historyDisplayNamesFollowActorType() {
        when(requestMapper.requestById(500L)).thenReturn(request(500L, ReviewStatus.APPROVED_CODE));
        when(actionMapper.selectByReviewRequestId(500L)).thenReturn(List.of(
                action(1L, ReviewActionType.SUBMITTED_CODE, 9L),
                action(2L, ReviewActionType.APPROVED_CODE, 1L)));

        ReviewDetailVO detail = service().getDetail(500L, viewer(1L, ReviewPermissions.READ));

        assertThat(detail.getHistory()).hasSize(2);
        ReviewActionVO submitted = detail.getHistory().get(0);
        ReviewActionVO approved = detail.getHistory().get(1);
        assertThat(submitted.getActorDisplayName()).isEqualTo("账户 #9");
        // 决策人只有 accountId，Account 模块未暴露账户摘要 API，因此这里保持 null
        assertThat(approved.getActorDisplayName()).isNull();
        assertThat(approved.getActorAccountId()).isEqualTo(1L);
    }

    // ---------- REV-007 ----------

    /**
     * \u5ba1\u6838\u5386\u53f2\uff1a\u6309\u76ee\u6807\u4e0e\u72b6\u6001\u7b5b\u9009\u5e76\u5206\u9875\u8fd4\u56de。
     * 主要协作调用：<code>requestMapper.historyCount</code>、<code>requestMapper.historyPage</code>。
     */
    @Test
    @DisplayName("审核历史：按目标与状态筛选并分页返回")
    void historyPageFiltersAndPages() {
        ReviewHistoryQueryDTO query = new ReviewHistoryQueryDTO();
        query.setTargetModule(ReviewDemoTargetHandler.TARGET_MODULE);
        query.setTargetType(ReviewDemoTargetHandler.TARGET_TYPE);
        query.setTargetId(100L);
        query.setStatus("approved");
        query.setPage(2);
        query.setPageSize(10);
        when(requestMapper.historyCount(eq(ReviewDemoTargetHandler.TARGET_MODULE),
                eq(ReviewDemoTargetHandler.TARGET_TYPE), eq(100L), isNull(), eq("APPROVED"),
                isNull(), isNull(), isNull(), isNull())).thenReturn(1L);
        when(requestMapper.historyPage(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                eq(10), eq(10))).thenReturn(List.of(request(500L, ReviewStatus.APPROVED_CODE)));

        PageResult<ReviewHistoryVO> result = service().pageHistory(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getItems().get(0).getStatus()).isEqualTo(ReviewStatus.APPROVED_CODE);
    }

    /**
     * \u5ba1\u6838\u5386\u53f2\uff1astatus \u53d6\u503c\u975e\u6cd5\u660e\u786e\u62a5\u9519\uff0c\u4e0d\u9759\u9ed8\u5ffd\u7565\u7b5b\u9009。
     * 关键处理步骤调用 <code>query.setStatus</code>、<code>service</code>。
     */
    @Test
    @DisplayName("审核历史：status 取值非法明确报错，不静默忽略筛选")
    void historyRejectsInvalidStatus() {
        ReviewHistoryQueryDTO query = new ReviewHistoryQueryDTO();
        query.setStatus("DONE");

        assertThatThrownBy(() -> service().pageHistory(query))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_QUERY_INVALID");
    }

    /**
     * \u5ba1\u6838\u5386\u53f2\uff1astartTime \u665a\u4e8e endTime \u660e\u786e\u62a5\u9519。
     * 关键处理步骤调用 <code>query.setStartTime</code>、<code>LocalDateTime.now</code>、<code>query.setEndTime</code>、<code>service</code>。
     */
    @Test
    @DisplayName("审核历史：startTime 晚于 endTime 明确报错")
    void historyRejectsInvertedTimeRange() {
        ReviewHistoryQueryDTO query = new ReviewHistoryQueryDTO();
        query.setStartTime(LocalDateTime.now());
        query.setEndTime(LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> service().pageHistory(query))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_QUERY_INVALID");
    }

    /**
     * \u5ba1\u6838\u5386\u53f2\uff1apageSize \u8d8a\u754c\u8fd4\u56de REVIEW_QUERY_INVALID。
     * 关键处理步骤调用 <code>query.setPageSize</code>、<code>service</code>。
     */
    @Test
    @DisplayName("审核历史：pageSize 越界返回 REVIEW_QUERY_INVALID")
    void historyRejectsInvalidPageSize() {
        ReviewHistoryQueryDTO query = new ReviewHistoryQueryDTO();
        query.setPageSize(0);

        assertThatThrownBy(() -> service().pageHistory(query))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_QUERY_INVALID");
    }
}
