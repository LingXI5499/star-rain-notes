package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starrainnotes.review.entity.ReviewActionEntity;
import com.starrainnotes.review.entity.ReviewRequestEntity;
import com.starrainnotes.review.mapper.ReviewActionMapper;
import com.starrainnotes.review.mapper.ReviewRequestMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * review 模块 2 个 Mapper（ReviewRequestMapper、ReviewActionMapper）的真实数据库读写验证。
 *
 * 这个模块的语句是全项目里最依赖「条件更新返回值」的：
 * 任何状态变更都必须走 *IfPending，affected_rows 为 1 才算这次决定生效。
 * 单元测试把 Mapper mock 掉之后，WHERE 里的 status = 'PENDING' 从来没有被真正执行过 ——
 * 一旦迁移时漏掉这个守卫，就会出现「同一份审核被批准两次 / 批完还能撤回」。
 * 所以本类把三条 *IfPending 的「第一次 1、第二次 0」逐个钉住，并回读最终状态。
 *
 * ReviewActionMapper 按设计只有 insert + select（历史只追加、不修改、不删除），
 * 所以它的写路径就是 insertAction；没有 update / delete 语句可测。
 *
 * 全程在一个不 commit 的 SqlSession 里跑，@AfterEach 回滚；最后一个用例用新连接复核。
 */
class ReviewMapperXmlTest extends MapperXmlIntegrationSupport {

    private static final AtomicLong PROBE = new AtomicLong(System.nanoTime() % 1_000_000L);

    private SqlSession session;
    private ReviewRequestMapper requests;
    private ReviewActionMapper actions;

    @BeforeEach
    void open() {
        session = openSession();
        requests = session.getMapper(ReviewRequestMapper.class);
        actions = session.getMapper(ReviewActionMapper.class);
    }

    @AfterEach
    void rollback() {
        if (session != null) {
            session.rollback();
            session.close();
        }
    }

    @Test
    void everyReviewMapperIsReachableThroughTheSession() {
        assertNotNull(requests);
        assertNotNull(actions);
    }

    @Test
    void insertedRequestRoundTripsEveryColumnAndBackfillsItsId() {
        LocalDateTime submittedAt = now().minusMinutes(3);
        ReviewRequestEntity request = newRequest("TUTORIAL_PUBLISH", "TUTORIAL", "TUTORIAL",
                probeId(), submittedAt, unique("mapperxmlreview"));
        assertEquals(1, requests.insertRequest(request));
        assertNotNull(request.getId(), "insertRequest 没有把自增主键回填到实体上");

        ReviewRequestEntity loaded = requests.requestById(request.getId());
        assertNotNull(loaded, "requestById 读不到刚插入的行");
        assertEquals("TUTORIAL_PUBLISH", loaded.getReviewType());
        assertEquals("TUTORIAL", loaded.getTargetModule());
        assertEquals("TUTORIAL", loaded.getTargetType());
        assertEquals(request.getTargetId(), loaded.getTargetId());
        assertEquals(request.getTargetRevisionRef(), loaded.getTargetRevisionRef());
        assertEquals(request.getTargetDisplayName(), loaded.getTargetDisplayName());
        assertEquals(request.getApplicantAccountId(), loaded.getApplicantAccountId());
        assertEquals("XML 验证申请人", loaded.getApplicantDisplayName());
        assertEquals("请在 XML 验证里审一下", loaded.getSubmissionNote());
        assertEquals("PENDING", loaded.getStatus());
        assertNull(loaded.getReviewerAccountId());
        assertNull(loaded.getDecisionReason());
        assertNull(loaded.getDecidedAt());
        assertNull(loaded.getCanceledAt());
        assertEquals(submittedAt, loaded.getSubmittedAt(),
                "submitted_at 没有按 datetime(3) 原样读回（检查列清单与毫秒截断）");
        assertNotNull(loaded.getCreatedAt(), "created_at 由列默认值填充，应能读回");

        assertNull(requests.requestById(-1L), "不存在的 id 应返回 null");
    }

    @Test
    void selectPendingPicksTheNewestPendingRowForOneTarget() {
        long targetId = probeId();
        String displayName = unique("mapperxmlpending");
        ReviewRequestEntity older = newRequest("TUTORIAL_PUBLISH", "TUTORIAL", "TUTORIAL", targetId,
                now().minusMinutes(2), displayName);
        requests.insertRequest(older);
        ReviewRequestEntity newer = newRequest("TUTORIAL_PUBLISH", "TUTORIAL", "TUTORIAL", targetId,
                now().minusMinutes(1), displayName);
        requests.insertRequest(newer);

        ReviewRequestEntity pending = requests.selectPending("TUTORIAL", "TUTORIAL", targetId, "TUTORIAL_PUBLISH");
        assertNotNull(pending, "同一目标 + 审核类型的待审请求应当能查到");
        assertEquals(newer.getId(), pending.getId(), "selectPending 必须按 id DESC 取最新的一条");

        assertNull(requests.selectPending("TUTORIAL", "TUTORIAL", targetId, "TUTORIAL_WITHDRAW"),
                "review_type 不匹配时不应命中");
        assertNull(requests.selectPending("BLOG", "TUTORIAL", targetId, "TUTORIAL_PUBLISH"),
                "target_module 不匹配时不应命中");
        assertNull(requests.selectPending("TUTORIAL", "TUTORIAL", probeId(), "TUTORIAL_PUBLISH"),
                "target_id 不匹配时不应命中");

        assertEquals(1, requests.approveIfPending(newer.getId(), probeId(), "通过", now()));
        assertNotNull(requests.selectPending("TUTORIAL", "TUTORIAL", targetId, "TUTORIAL_PUBLISH"),
                "仍有更早那条 PENDING，selectPending 应当回退到它");
        assertEquals(older.getId(),
                requests.selectPending("TUTORIAL", "TUTORIAL", targetId, "TUTORIAL_PUBLISH").getId());
        assertEquals(1, requests.approveIfPending(older.getId(), probeId(), "通过", now()));
        assertNull(requests.selectPending("TUTORIAL", "TUTORIAL", targetId, "TUTORIAL_PUBLISH"),
                "两条都批完以后不应该再查到待审请求");
    }

    @Test
    void pendingListHonoursItsThreeOptionalFiltersAndOrdering() {
        String keyword = unique("mapperxmlpendingpage");
        String module = unique("MAPPERXMLMODULE");
        String reviewType = unique("MAPPERXMLTYPE");
        ReviewRequestEntity first = newRequest(reviewType, module, "TUTORIAL", probeId(),
                now().minusMinutes(3), keyword + "-first");
        requests.insertRequest(first);
        ReviewRequestEntity second = newRequest(reviewType, module, "TUTORIAL", probeId(),
                now().minusMinutes(2), keyword + "-second");
        requests.insertRequest(second);
        ReviewRequestEntity other = newRequest(reviewType, module, "TUTORIAL", probeId(),
                now().minusMinutes(1), unique("mapperxmlpendingpage-other"));
        requests.insertRequest(other);

        assertEquals(3, requests.pendingCount(module, reviewType, null), "target_module 过滤没生效");
        assertEquals(2, requests.pendingCount(module, null, keyword),
                "keyword 应当匹配 target_display_name（只有前两条带这个标记）");
        assertEquals(3, requests.pendingCount(module, reviewType, "XML 验证申请人"),
                "keyword 必须同样匹配 applicant_display_name（三条都是同一个申请人）");
        assertEquals(0, requests.pendingCount(module, reviewType, keyword + "-missing"));
        assertEquals(0, requests.pendingCount(unique("MAPPERXMLMODULE-missing"), reviewType, null));
        assertEquals(0, requests.pendingCount(module, unique("MAPPERXMLTYPE-missing"), null));
        assertTrue(requests.pendingCount(null, null, null) >= 3,
                "条件全为 null 时必须退化成统计全部待审");

        List<ReviewRequestEntity> page = requests.pendingPage(module, reviewType, null, 0, 10);
        assertEquals(3, page.size());
        assertEquals(first.getId(), page.get(0).getId(), "待办必须按 submitted_at ASC, id ASC（先来先审）");
        assertEquals(second.getId(), page.get(1).getId());
        assertEquals(other.getId(), page.get(2).getId());
        assertEquals(1, requests.pendingPage(module, reviewType, null, 0, 1).size(), "LIMIT 没生效");
        assertEquals(second.getId(), requests.pendingPage(module, reviewType, null, 1, 1).get(0).getId(),
                "OFFSET 没生效");
        assertEquals(2, requests.pendingPage(module, null, keyword, 0, 10).size());
    }

    @Test
    void everyDecisionGuardAppliesExactlyOnce() {
        ReviewRequestEntity approvable = newRequest("TUTORIAL_PUBLISH", "TUTORIAL", "TUTORIAL",
                probeId(), now(), unique("mapperxmlapprove"));
        requests.insertRequest(approvable);
        long reviewer = probeId();
        LocalDateTime decidedAt = now();

        assertEquals(1, requests.approveIfPending(approvable.getId(), reviewer, "内容达标", decidedAt));
        assertEquals(0, requests.approveIfPending(approvable.getId(), reviewer, "再批一次", now()),
                "已经批准过的请求不能被二次批准");
        ReviewRequestEntity approved = requests.requestById(approvable.getId());
        assertEquals("APPROVED", approved.getStatus());
        assertEquals(reviewer, approved.getReviewerAccountId().longValue());
        assertEquals("内容达标", approved.getDecisionReason());
        assertEquals(decidedAt, approved.getDecidedAt());
        assertNull(approved.getCanceledAt());
        assertEquals(0, requests.rejectIfPending(approvable.getId(), reviewer, "改成驳回", now()),
                "已批准的不能被驳回");
        assertEquals(0, requests.cancelIfPending(approvable.getId(), now()), "已批准的不能被取消");
        assertEquals("APPROVED", requests.requestById(approvable.getId()).getStatus(),
                "被拒绝的决定不应改动状态");

        ReviewRequestEntity rejectable = newRequest("TUTORIAL_PUBLISH", "TUTORIAL", "TUTORIAL",
                probeId(), now(), unique("mapperxmlreject"));
        requests.insertRequest(rejectable);
        assertEquals(1, requests.rejectIfPending(rejectable.getId(), reviewer, "排版不合格", now()));
        assertEquals(0, requests.rejectIfPending(rejectable.getId(), reviewer, "再驳一次", now()));
        ReviewRequestEntity rejected = requests.requestById(rejectable.getId());
        assertEquals("REJECTED", rejected.getStatus());
        assertEquals("排版不合格", rejected.getDecisionReason());
        assertNotNull(rejected.getDecidedAt());

        ReviewRequestEntity cancelable = newRequest("TUTORIAL_PUBLISH", "TUTORIAL", "TUTORIAL",
                probeId(), now(), unique("mapperxmlcancel"));
        requests.insertRequest(cancelable);
        LocalDateTime canceledAt = now();
        assertEquals(1, requests.cancelIfPending(cancelable.getId(), canceledAt));
        assertEquals(0, requests.cancelIfPending(cancelable.getId(), canceledAt), "已取消的不能再取消");
        ReviewRequestEntity canceled = requests.requestById(cancelable.getId());
        assertEquals("CANCELED", canceled.getStatus());
        assertEquals(canceledAt, canceled.getCanceledAt());
        /*
         * cancelIfPending 会把决策三列显式清成 NULL。PENDING 行本来这三列就是 NULL，
         * 所以这里只能证明「取消后它们仍是 NULL」——要构造「PENDING 但带着决策字段」的状态
         * 需要绕过 Mapper，刻意不做。
         */
        assertNull(canceled.getReviewerAccountId());
        assertNull(canceled.getDecisionReason());
        assertNull(canceled.getDecidedAt());

        assertEquals(0, requests.approveIfPending(-1L, reviewer, "不存在", now()));
        assertEquals(0, requests.rejectIfPending(-1L, reviewer, "不存在", now()));
        assertEquals(0, requests.cancelIfPending(-1L, now()));
    }

    @Test
    void historyFiltersAndOrderingCoverEveryOptionalCondition() {
        String module = unique("MAPPERXMLHISTORY");
        String reviewType = unique("MAPPERXMLHISTORYTYPE");
        long applicant = probeId();
        long reviewer = probeId();
        LocalDateTime base = now().minusMinutes(10);

        ReviewRequestEntity first = newRequest(reviewType, module, "TUTORIAL", probeId(), base,
                unique("mapperxmlhistory"));
        first.setApplicantAccountId(applicant);
        requests.insertRequest(first);
        ReviewRequestEntity second = newRequest(reviewType, module, "TUTORIAL", probeId(),
                base.plusMinutes(1), unique("mapperxmlhistory"));
        second.setApplicantAccountId(applicant);
        requests.insertRequest(second);
        ReviewRequestEntity third = newRequest(reviewType, module, "TUTORIAL", probeId(),
                base.plusMinutes(2), unique("mapperxmlhistory"));
        third.setApplicantAccountId(probeId());
        requests.insertRequest(third);

        assertEquals(1, requests.rejectIfPending(first.getId(), reviewer, "驳回", base.plusMinutes(3)));
        assertEquals(1, requests.cancelIfPending(third.getId(), base.plusMinutes(4)));

        assertEquals(3, requests.historyCount(module, null, null, null, null, null, null, null, null));
        assertEquals(1, requests.historyCount(module, "TUTORIAL", first.getTargetId(), reviewType,
                "REJECTED", null, null, null, null), "target_id + status 过滤没生效");
        assertEquals(0, requests.historyCount(module, "TUTORIAL", first.getTargetId(), reviewType,
                "APPROVED", null, null, null, null));
        assertEquals(2, requests.historyCount(module, null, null, null, null, applicant, null, null, null),
                "applicantAccountId 过滤没生效");
        assertEquals(1, requests.historyCount(module, null, null, null, null, null, reviewer, null, null),
                "reviewerAccountId 过滤没生效（只统计真正决策过的那条）");
        assertEquals(3, requests.historyCount(module, null, null, null, null, null, null,
                base.minusMinutes(1), null), "startTime 过滤（submitted_at >= start）没生效");
        assertEquals(3, requests.historyCount(module, null, null, null, null, null, null,
                null, base.plusMinutes(5)), "endTime 过滤（submitted_at <= end）没生效");
        assertEquals(0, requests.historyCount(module, null, null, null, null, null, null,
                null, base.minusMinutes(1)), "endTime 早于所有提交时间时应当为 0");
        assertEquals(1, requests.historyCount(module, "TUTORIAL", null, null, null, null, null,
                base.plusMinutes(1), base.plusMinutes(1)), "startTime = endTime 应当只命中一条");
        assertEquals(0, requests.historyCount(unique("MAPPERXMLHISTORY-missing"), null, null, null,
                null, null, null, null, null));

        List<ReviewRequestEntity> page = requests.historyPage(module, null, null, null, null, null,
                null, null, null, 0, 10);
        assertEquals(3, page.size());
        assertEquals(third.getId(), page.get(0).getId(), "历史必须按 submitted_at DESC, id DESC");
        assertEquals(first.getId(), page.get(2).getId());
        assertEquals(1, requests.historyPage(module, null, null, null, null, null, null, null, null, 0, 1)
                .size(), "LIMIT 没生效");
        assertEquals(second.getId(),
                requests.historyPage(module, null, null, null, null, null, null, null, null, 1, 1)
                        .get(0).getId(), "OFFSET 没生效");
    }

    @Test
    void targetNamedLockCanBeAcquiredAndReleased() {
        String lockName = "mapper-xml-test-lock-" + unique("review");
        assertEquals(1, requests.acquireTargetLock(lockName, 1), "GET_LOCK 应当立刻拿到锁");
        assertEquals(1, requests.releaseTargetLock(lockName), "RELEASE_LOCK 应当成功释放自己拿到的锁");
    }

    @Test
    void actionHistoryAppendsInOrderAndHasNoUpdateOrDelete() {
        ReviewRequestEntity request = newRequest("TUTORIAL_PUBLISH", "TUTORIAL", "TUTORIAL",
                probeId(), now(), unique("mapperxmlaction"));
        requests.insertRequest(request);
        assertTrue(actions.selectByReviewRequestId(request.getId()).isEmpty(),
                "刚创建的请求还没有动作历史");

        ReviewActionEntity submitted = newAction(request.getId(), "SUBMITTED", probeId(), "ACCOUNT", "提交审核");
        assertEquals(1, actions.insertAction(submitted));
        assertNotNull(submitted.getId(), "insertAction 没有回填自增主键");
        ReviewActionEntity approved = newAction(request.getId(), "APPROVED", null, "SYSTEM", null);
        assertEquals(1, actions.insertAction(approved));
        assertNotNull(approved.getId());

        List<ReviewActionEntity> history = actions.selectByReviewRequestId(request.getId());
        assertEquals(2, history.size());
        assertEquals(submitted.getId(), history.get(0).getId(), "动作历史必须按 created_at,id 升序");
        assertEquals("SUBMITTED", history.get(0).getActionType());
        assertEquals("ACCOUNT", history.get(0).getActorType());
        assertEquals("提交审核", history.get(0).getNote());
        assertNotNull(history.get(0).getCreatedAt(), "created_at 由列默认值填充，应能读回");
        assertEquals("APPROVED", history.get(1).getActionType());
        assertEquals("SYSTEM", history.get(1).getActorType());
        assertNull(history.get(1).getActorAccountId(), "系统动作允许没有自然人");
        assertNull(history.get(1).getNote());

        assertTrue(actions.selectByReviewRequestId(probeId()).isEmpty(),
                "别人的审核请求不应带出动作历史");
    }

    @Test
    void rollbackLeavesNoReviewProbeRowsBehind() {
        ReviewRequestEntity request = newRequest("TUTORIAL_PUBLISH", "TUTORIAL", "TUTORIAL",
                probeId(), now(), unique("mapperxmlrollback"));
        requests.insertRequest(request);
        ReviewActionEntity action = newAction(request.getId(), "SUBMITTED", probeId(), "ACCOUNT", null);
        actions.insertAction(action);
        Long requestId = request.getId();
        assertNotNull(requestId);
        assertNotNull(action.getId());

        session.rollback();
        try (SqlSession fresh = openSession()) {
            assertNull(fresh.getMapper(ReviewRequestMapper.class).requestById(requestId),
                    "回滚后不应在开发库里留下测试审核请求");
            assertTrue(fresh.getMapper(ReviewActionMapper.class).selectByReviewRequestId(requestId).isEmpty(),
                    "回滚后不应在开发库里留下测试审核动作");
        }
    }

    private ReviewRequestEntity newRequest(String reviewType, String module, String type, long targetId,
                                           LocalDateTime submittedAt, String displayName) {
        ReviewRequestEntity request = new ReviewRequestEntity();
        request.setReviewType(reviewType);
        request.setTargetModule(module);
        request.setTargetType(type);
        request.setTargetId(targetId);
        request.setTargetRevisionRef("mapper-xml-test:revision:" + PROBE.incrementAndGet());
        request.setTargetDisplayName(displayName);
        request.setApplicantAccountId(probeId());
        request.setApplicantDisplayName("XML 验证申请人");
        request.setSubmissionNote("请在 XML 验证里审一下");
        request.setStatus("PENDING");
        request.setSubmittedAt(submittedAt);
        return request;
    }

    private ReviewActionEntity newAction(Long requestId, String type, Long actorId, String actorType, String note) {
        ReviewActionEntity action = new ReviewActionEntity();
        action.setReviewRequestId(requestId);
        action.setActionType(type);
        action.setActorAccountId(actorId);
        action.setActorType(actorType);
        action.setNote(note);
        action.setCreatedAt(now());
        return action;
    }

    /* 逻辑外键，库里没有物理 FOREIGN KEY：用一个大号段避免和真实账户/内容撞号 */
    private long probeId() {
        return 900_000_000_000L + PROBE.incrementAndGet();
    }

    private String unique(String prefix) {
        return prefix + "-" + PROBE.incrementAndGet() + "-" + System.nanoTime();
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
