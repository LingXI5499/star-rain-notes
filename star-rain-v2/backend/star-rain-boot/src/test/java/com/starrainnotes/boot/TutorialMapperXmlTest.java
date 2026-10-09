package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starrainnotes.tutorial.content.entity.TutorialCategoryEntity;
import com.starrainnotes.tutorial.content.dto.TutorialDashboardStatsDTO;
import com.starrainnotes.tutorial.content.entity.TutorialChapterEntity;
import com.starrainnotes.tutorial.content.entity.TutorialEntity;
import com.starrainnotes.tutorial.content.entity.TutorialGroupEntity;
import com.starrainnotes.tutorial.content.entity.TutorialKnowledgeCardEntity;
import com.starrainnotes.tutorial.content.entity.TutorialQuestionEntity;
import com.starrainnotes.tutorial.content.entity.TutorialRevisionEntity;
import com.starrainnotes.tutorial.content.mapper.TutorialCategoryMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialChapterMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialGroupMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialKnowledgeCardMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialQuestionMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialRevisionMapper;
import com.starrainnotes.tutorial.learning.entity.KnowledgeMasteryEntity;
import com.starrainnotes.tutorial.learning.entity.LearningHistoryEntity;
import com.starrainnotes.tutorial.learning.entity.LearningProgressEntity;
import com.starrainnotes.tutorial.learning.entity.ReviewResultEntity;
import com.starrainnotes.tutorial.learning.entity.ReviewScheduleEntity;
import com.starrainnotes.tutorial.learning.entity.ReviewTaskEntity;
import com.starrainnotes.tutorial.learning.entity.StudyPlanEntity;
import com.starrainnotes.tutorial.learning.entity.StudyTaskEntity;
import com.starrainnotes.tutorial.learning.entity.UserQuestionAnswerEntity;
import com.starrainnotes.tutorial.learning.mapper.KnowledgeMasteryMapper;
import com.starrainnotes.tutorial.learning.mapper.LearningHistoryMapper;
import com.starrainnotes.tutorial.learning.mapper.LearningProgressMapper;
import com.starrainnotes.tutorial.learning.mapper.ReviewResultMapper;
import com.starrainnotes.tutorial.learning.mapper.ReviewScheduleMapper;
import com.starrainnotes.tutorial.learning.mapper.ReviewTaskMapper;
import com.starrainnotes.tutorial.learning.mapper.StudyPlanMapper;
import com.starrainnotes.tutorial.learning.mapper.StudyTaskMapper;
import com.starrainnotes.tutorial.learning.mapper.UserQuestionAnswerMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * star-rain-tutorial 16 个 Mapper 的真实数据库读写验证。
 *
 * 覆盖 Mockito 单元测试永远测不到的那一层：XML 的列清单、自增主键回填、可空列的映射、
 * `INSERT IGNORE` / `ON DUPLICATE KEY UPDATE` 的幂等性、以及两条从 BaseMapper 迁移过来
 * 时最容易悄悄改变的语义：
 *   1. TutorialEntity.withdrawnAt / TutorialChapterEntity.summary 原先带
 *      @TableField(updateStrategy = FieldStrategy.ALWAYS)，「传 null 也要写进 UPDATE」；
 *   2. StudyPlanEntity.endDate / dailyTargetMinutes 原先走默认 NOT_NULL 策略，
 *      「传 null 时保留库里的旧值」。
 *
 * 全程在一个不 commit 的 SqlSession 里跑，@AfterEach 回滚；另有一个用例显式用新连接
 * 断言回滚后库里没有残留测试数据。
 */
class TutorialMapperXmlTest extends MapperXmlIntegrationSupport {

    private static final AtomicLong PROBE = new AtomicLong(System.nanoTime() % 1_000_000L);

    private SqlSession session;
    private TutorialCategoryMapper categories;
    private TutorialMapper tutorials;
    private TutorialGroupMapper groups;
    private TutorialChapterMapper chapters;
    private TutorialKnowledgeCardMapper cards;
    private TutorialQuestionMapper questions;
    private TutorialRevisionMapper revisions;
    private StudyPlanMapper plans;
    private StudyTaskMapper tasks;
    private LearningProgressMapper progress;
    private LearningHistoryMapper history;
    private KnowledgeMasteryMapper mastery;
    private ReviewTaskMapper reviewTasks;
    private ReviewScheduleMapper schedules;
    private ReviewResultMapper reviewResults;
    private UserQuestionAnswerMapper answers;

    @BeforeEach
    void open() {
        session = openSession();
        categories = session.getMapper(TutorialCategoryMapper.class);
        tutorials = session.getMapper(TutorialMapper.class);
        groups = session.getMapper(TutorialGroupMapper.class);
        chapters = session.getMapper(TutorialChapterMapper.class);
        cards = session.getMapper(TutorialKnowledgeCardMapper.class);
        questions = session.getMapper(TutorialQuestionMapper.class);
        revisions = session.getMapper(TutorialRevisionMapper.class);
        plans = session.getMapper(StudyPlanMapper.class);
        tasks = session.getMapper(StudyTaskMapper.class);
        progress = session.getMapper(LearningProgressMapper.class);
        history = session.getMapper(LearningHistoryMapper.class);
        mastery = session.getMapper(KnowledgeMasteryMapper.class);
        reviewTasks = session.getMapper(ReviewTaskMapper.class);
        schedules = session.getMapper(ReviewScheduleMapper.class);
        reviewResults = session.getMapper(ReviewResultMapper.class);
        answers = session.getMapper(UserQuestionAnswerMapper.class);
    }

    @AfterEach
    void rollback() {
        if (session != null) {
            session.rollback();
            session.close();
        }
    }

    @Test
    void everyTutorialMapperIsReachableThroughTheSession() {
        assertNotNull(categories);
        assertNotNull(tutorials);
        assertNotNull(groups);
        assertNotNull(chapters);
        assertNotNull(cards);
        assertNotNull(questions);
        assertNotNull(revisions);
        assertNotNull(plans);
        assertNotNull(tasks);
        assertNotNull(progress);
        assertNotNull(history);
        assertNotNull(mastery);
        assertNotNull(reviewTasks);
        assertNotNull(schedules);
        assertNotNull(reviewResults);
        assertNotNull(answers);
    }

    @Test
    void categoryMapperRoundTripsCrudAndOrdering() {
        TutorialCategoryEntity row = newCategory(unique("mapper-xml-test-category"));
        assertEquals(1, categories.insert(row));
        assertNotNull(row.getId(), "insert 没有回填自增主键");

        TutorialCategoryEntity loaded = categories.selectById(row.getId());
        assertNotNull(loaded);
        assertEquals(row.getName(), loaded.getName());
        assertEquals(row.getSlug(), loaded.getSlug());
        assertEquals(10, loaded.getSortOrder().intValue());
        assertNotNull(loaded.getCreatedAt(), "created_at 没有映射回来，检查列清单");
        assertNotNull(loaded.getUpdatedAt());
        assertEquals(1, categories.countBySlug(row.getSlug()));
        assertTrue(categories.listOrdered().stream().anyMatch(item -> item.getId().equals(row.getId())));

        LocalDateTime at = now();
        assertEquals(1, categories.updateName(row.getId(), "改名后的知识体系", at));
        assertEquals(1, categories.updateSortOrder(row.getId(), 99, at));
        TutorialCategoryEntity updated = categories.selectById(row.getId());
        assertEquals("改名后的知识体系", updated.getName());
        assertEquals(99, updated.getSortOrder().intValue());
        assertEquals(row.getSlug(), updated.getSlug(), "改名不能改动公开地址 slug");

        assertEquals(1, categories.deleteById(row.getId()));
        assertNull(categories.selectById(row.getId()));
    }

    @Test
    void dashboardQueriesCountDraftChaptersAndLimitRecentRows() {
        TutorialDashboardStatsDTO before = tutorials.dashboardStats();
        TutorialCategoryEntity category = newCategory(unique("dashboard-category"));
        categories.insert(category);
        TutorialEntity tutorial = newTutorial(category.getId(), unique("dashboard-tutorial"));
        tutorials.insert(tutorial);
        TutorialGroupEntity group = newGroup(tutorial.getId(), "概览分组");
        groups.insert(group);
        TutorialChapterEntity chapter = newChapter(tutorial.getId(), group.getId(), unique("dashboard-chapter"));
        chapters.insert(chapter);

        TutorialDashboardStatsDTO after = tutorials.dashboardStats();
        assertEquals(before.getTotal() + 1, after.getTotal());
        assertEquals(before.getDrafts() + 1, after.getDrafts());
        assertEquals(before.getChapters() + 1, after.getChapters());
        assertEquals(before.getDraftChapters() + 1, after.getDraftChapters());
        List<TutorialEntity> recent = tutorials.dashboardRecent();
        assertTrue(recent.size() <= 8);
        assertTrue(recent.stream().allMatch(row -> row.getId() != null
            && row.getTitle() != null && row.getUpdatedAt() != null));
    }

    @Test
    void tutorialMapperRoundTripsWorkspaceRowAndClearsWithdrawnAt() {
        TutorialCategoryEntity category = newCategory(unique("mapper-xml-test-tutorial-category"));
        categories.insert(category);
        String slug = unique("mapper-xml-test-tutorial");
        TutorialEntity row = newTutorial(category.getId(), slug);
        assertEquals(1, tutorials.insert(row));
        assertNotNull(row.getId(), "insert 没有回填自增主键");

        TutorialEntity loaded = tutorials.selectById(row.getId());
        assertNotNull(loaded);
        assertEquals("NEVER_PUBLISHED", loaded.getPublicationStatus());
        assertEquals("DRAFT", loaded.getEditingStatus());
        assertNotNull(loaded.getCreatedAt());
        assertNull(loaded.getWithdrawnAt());
        assertNotNull(tutorials.byIdForUpdate(row.getId()), "FOR UPDATE 读不到刚插入的行");
        assertEquals(1, tutorials.countBySlug(slug));
        assertEquals(1, tutorials.countByCategoryId(category.getId()));
        assertTrue(tutorials.listOrdered(category.getId()).stream().anyMatch(item -> item.getId().equals(row.getId())));
        assertTrue(tutorials.listOrdered(null).stream().anyMatch(item -> item.getId().equals(row.getId())));
        assertNull(tutorials.selectPublishedBySlug(slug), "未发布教程不应出现在公开读取里");

        // 撤回：withdrawn_at 写入真实时间
        loaded.setPublicationStatus("WITHDRAWN");
        loaded.setWithdrawnAt(now());
        loaded.setUpdatedAt(now());
        assertEquals(1, tutorials.update(loaded));
        assertNotNull(tutorials.selectById(row.getId()).getWithdrawnAt());

        // 重新发布：withdrawn_at 必须能被清成 NULL（旧 @TableField(ALWAYS) 语义）
        loaded = tutorials.selectById(row.getId());
        loaded.setPublicationStatus("PUBLISHED");
        loaded.setWithdrawnAt(null);
        loaded.setUpdatedAt(now());
        assertEquals(1, tutorials.update(loaded));
        assertNull(tutorials.selectById(row.getId()).getWithdrawnAt(),
                "withdrawn_at 传 null 没有清空，ALWAYS 更新语义在迁移中丢了");
        assertFalse(tutorials.listPublishedWithRevision().stream().anyMatch(item -> item.getId().equals(row.getId())),
                "还没有公开版本时不应出现在公开目录里");

        TutorialRevisionEntity revision = newRevision(row.getId(), 1);
        assertEquals(1, revisions.insert(revision));
        loaded = tutorials.selectById(row.getId());
        loaded.setPublishedRevisionId(revision.getId());
        loaded.setUpdatedAt(now());
        assertEquals(1, tutorials.update(loaded));

        assertNotNull(tutorials.selectPublishedBySlug(slug));
        assertTrue(tutorials.listPublishedWithRevision().stream().anyMatch(item -> item.getId().equals(row.getId())));
        assertTrue(tutorials.listPublished().stream().anyMatch(item -> item.getId().equals(row.getId())));
    }

    @Test
    void groupMapperRoundTripsTitleStatusAndOrder() {
        TutorialEntity tutorial = insertTutorial();
        TutorialGroupEntity group = newGroup(tutorial.getId(), "第一个分组");
        assertEquals(1, groups.insert(group));
        assertNotNull(group.getId());

        TutorialGroupEntity loaded = groups.selectById(group.getId());
        assertNotNull(loaded);
        assertEquals("ACTIVE", loaded.getStatus());
        assertEquals(10, loaded.getSortOrder().intValue());
        assertNotNull(loaded.getCreatedAt());
        assertTrue(groups.listByTutorialId(tutorial.getId()).stream().anyMatch(item -> item.getId().equals(group.getId())));
        assertTrue(groups.listActiveByTutorialId(tutorial.getId()).stream().anyMatch(item -> item.getId().equals(group.getId())));
        assertEquals(1, groups.countByTutorialId(tutorial.getId()));

        LocalDateTime at = now();
        assertEquals(1, groups.updateTitle(group.getId(), "改名后的分组", at));
        assertEquals("改名后的分组", groups.selectById(group.getId()).getTitle());
        assertEquals(1, groups.updateSortOrder(group.getId(), 25, at));
        assertEquals(25, groups.selectById(group.getId()).getSortOrder().intValue());
        assertEquals(1, groups.updateStatus(group.getId(), "ARCHIVED", at));
        assertEquals("ARCHIVED", groups.selectById(group.getId()).getStatus());
        assertTrue(groups.listActiveByTutorialId(tutorial.getId()).stream()
                .noneMatch(item -> item.getId().equals(group.getId())));
    }

    @Test
    void chapterMapperRoundTripsBodyStatusMoveAndClearsSummary() {
        TutorialEntity tutorial = insertTutorial();
        TutorialGroupEntity group = newGroup(tutorial.getId(), "分组");
        groups.insert(group);
        TutorialGroupEntity other = newGroup(tutorial.getId(), "另一个分组");
        groups.insert(other);

        String slug = unique("mapper-xml-test-chapter");
        TutorialChapterEntity chapter = newChapter(tutorial.getId(), group.getId(), slug);
        chapter.setSummary("初始摘要");
        assertEquals(1, chapters.insert(chapter));
        assertNotNull(chapter.getId(), "insert 没有回填自增主键");

        TutorialChapterEntity loaded = chapters.selectById(chapter.getId());
        assertNotNull(loaded);
        assertEquals("初始摘要", loaded.getSummary());
        assertEquals("# 正文", loaded.getBodyMarkdown());
        assertEquals("PUBLISHED", loaded.getStatus());
        assertEquals(1, chapters.countByTutorialId(tutorial.getId()));
        assertEquals(1, chapters.countActiveByGroupId(group.getId()));
        assertEquals(1, chapters.countByTutorialIdAndSlug(tutorial.getId(), slug));
        assertTrue(chapters.listByGroupId(group.getId()).stream().anyMatch(item -> item.getId().equals(chapter.getId())));
        assertTrue(chapters.listActiveByGroupId(group.getId()).stream().anyMatch(item -> item.getId().equals(chapter.getId())));

        // summary 是「允许被清空」的列（旧 @TableField(ALWAYS)）
        LocalDateTime at = now();
        assertEquals(1, chapters.updateContent(chapter.getId(), "新标题", null, at));
        TutorialChapterEntity cleared = chapters.selectById(chapter.getId());
        assertEquals("新标题", cleared.getTitle());
        assertNull(cleared.getSummary(), "summary 传 null 没有被清空，ALWAYS 更新语义在迁移中丢了");

        assertEquals(1, chapters.updateBody(chapter.getId(), "# 新正文", at));
        assertEquals("# 新正文", chapters.selectById(chapter.getId()).getBodyMarkdown());

        assertEquals(1, chapters.updateSortOrder(chapter.getId(), 77, at));
        assertEquals(77, chapters.selectById(chapter.getId()).getSortOrder().intValue());

        // V2_026 把章节状态统一成 DRAFT/PUBLISHED/WITHDRAWN（原来的 ACTIVE/ARCHIVED 已迁移）
        assertEquals(1, chapters.updateStatus(chapter.getId(), "WITHDRAWN", at));
        assertEquals(0, chapters.countActiveByGroupId(group.getId()));
        assertEquals(1, chapters.countByTutorialId(tutorial.getId()), "撤回章节不应从工作区计数里消失");
        assertEquals(1, chapters.updateStatus(chapter.getId(), "PUBLISHED", at));

        assertEquals(1, chapters.moveToGroup(chapter.getId(), other.getId(), 33, at));
        TutorialChapterEntity moved = chapters.selectById(chapter.getId());
        assertEquals(other.getId(), moved.getGroupId());
        assertEquals(33, moved.getSortOrder().intValue());
        assertTrue(chapters.listByGroupId(other.getId()).stream().anyMatch(item -> item.getId().equals(chapter.getId())));
        assertTrue(chapters.listByGroupId(group.getId()).stream().noneMatch(item -> item.getId().equals(chapter.getId())));
    }

    @Test
    void knowledgeCardMapperRoundTripsContentStatusAndDelete() {
        TutorialChapterEntity chapter = insertChapter();

        TutorialKnowledgeCardEntity card = new TutorialKnowledgeCardEntity();
        card.setChapterId(chapter.getId());
        card.setFrontText("正面");
        card.setBackMarkdown("背面");
        card.setSortOrder(10);
        card.setStatus("ENABLED");
        assertEquals(1, cards.insert(card));
        assertNotNull(card.getId(), "insert 没有回填自增主键");

        TutorialKnowledgeCardEntity loaded = cards.selectById(card.getId());
        assertNotNull(loaded);
        assertEquals("正面", loaded.getFrontText());
        assertEquals("背面", loaded.getBackMarkdown());
        assertEquals("ENABLED", loaded.getStatus());
        assertNotNull(loaded.getCreatedAt());
        assertEquals(1, cards.listByChapterId(chapter.getId()).size());
        assertEquals(1, cards.listEnabledByChapterId(chapter.getId()).size());

        LocalDateTime at = now();
        assertEquals(1, cards.updateContent(card.getId(), "新正面", "新背面", "DISABLED", at));
        TutorialKnowledgeCardEntity disabled = cards.selectById(card.getId());
        assertEquals("新正面", disabled.getFrontText());
        assertEquals("DISABLED", disabled.getStatus());
        assertEquals(0, cards.listEnabledByChapterId(chapter.getId()).size(), "DISABLED 卡片不应进入发布快照");
        assertEquals(1, cards.listByChapterId(chapter.getId()).size());

        assertEquals(1, cards.updateSortOrder(card.getId(), 55));
        assertEquals(55, cards.selectById(card.getId()).getSortOrder().intValue());

        assertEquals(1, cards.deleteById(card.getId()));
        assertNull(cards.selectById(card.getId()));
    }

    @Test
    void questionMapperRoundTripsContentStatusAndDelete() {
        TutorialChapterEntity chapter = insertChapter();

        TutorialQuestionEntity question = new TutorialQuestionEntity();
        question.setChapterId(chapter.getId());
        question.setQuestionText("问题");
        question.setReferenceAnswer("参考答案");
        question.setSortOrder(10);
        question.setStatus("ENABLED");
        assertEquals(1, questions.insert(question));
        assertNotNull(question.getId(), "insert 没有回填自增主键");

        TutorialQuestionEntity loaded = questions.selectById(question.getId());
        assertNotNull(loaded);
        assertEquals("问题", loaded.getQuestionText());
        assertEquals("参考答案", loaded.getReferenceAnswer());
        assertEquals(1, questions.listByChapterId(chapter.getId()).size());
        assertEquals(1, questions.listEnabledByChapterId(chapter.getId()).size());

        LocalDateTime at = now();
        assertEquals(1, questions.updateContent(question.getId(), "新问题", "新答案", "DISABLED", at));
        TutorialQuestionEntity disabled = questions.selectById(question.getId());
        assertEquals("新问题", disabled.getQuestionText());
        assertEquals("DISABLED", disabled.getStatus());
        assertEquals(0, questions.listEnabledByChapterId(chapter.getId()).size());

        assertEquals(1, questions.updateSortOrder(question.getId(), 55));
        assertEquals(55, questions.selectById(question.getId()).getSortOrder().intValue());

        assertEquals(1, questions.deleteById(question.getId()));
        assertNull(questions.selectById(question.getId()));
    }

    @Test
    void revisionMapperRoundTripsSnapshotRefAndLatest() {
        TutorialEntity tutorial = insertTutorial();

        TutorialRevisionEntity first = newRevision(tutorial.getId(), 1);
        assertEquals(1, revisions.insert(first));
        assertNotNull(first.getId(), "insert 没有回填自增主键");
        TutorialRevisionEntity second = newRevision(tutorial.getId(), 2);
        assertEquals(1, revisions.insert(second));

        assertEquals(2, revisions.countByTutorialId(tutorial.getId()));
        TutorialRevisionEntity loaded = revisions.selectById(first.getId());
        assertNotNull(loaded);
        assertTrue(loaded.getSnapshotJson().contains("快照"), "JSON 列没有按字符串映射回来");
        assertEquals(first.getContentSha256(), loaded.getContentSha256());
        assertNotNull(loaded.getCreatedAt());

        TutorialRevisionEntity byRef = revisions.selectByTutorialIdAndRef(tutorial.getId(), first.getRevisionRef());
        assertNotNull(byRef);
        assertEquals(first.getId(), byRef.getId());
        assertNull(revisions.selectByTutorialIdAndRef(tutorial.getId(), unique("no-such-ref")));

        assertEquals(second.getId(), revisions.selectLatestByTutorialId(tutorial.getId()).getId(),
                "最新版本必须按 revision_no 倒序取一条");
    }

    @Test
    void studyPlanMapperKeepsNotNullSemanticsForOptionalColumns() {
        Long accountId = probeId();
        StudyPlanEntity plan = newPlan(accountId);
        assertEquals(1, plans.insert(plan));
        assertNotNull(plan.getId(), "insert 没有回填自增主键");

        StudyPlanEntity loaded = plans.selectById(plan.getId());
        assertNotNull(loaded);
        assertEquals("DRAFT", loaded.getStatus());
        assertEquals(0, loaded.getGeneratedVersion().intValue());
        assertNotNull(loaded.getStartDate());
        assertNull(loaded.getEndDate());
        assertNull(loaded.getDailyTargetMinutes());
        assertEquals(0, plans.countActiveByAccountId(accountId));
        assertTrue(plans.listRecentByAccountId(accountId).stream().anyMatch(item -> item.getId().equals(plan.getId())));

        loaded.setName("改名后的计划");
        loaded.setEndDate(LocalDate.now().plusDays(30));
        loaded.setDailyTargetMinutes(60);
        assertEquals(1, plans.updateDefinition(loaded));
        StudyPlanEntity saved = plans.selectById(plan.getId());
        assertEquals("改名后的计划", saved.getName());
        assertEquals(60, saved.getDailyTargetMinutes().intValue());
        assertNotNull(saved.getEndDate());

        // 可选列传 null 时保留旧值（原先 updateById 的 NOT_NULL 策略）
        saved.setEndDate(null);
        saved.setDailyTargetMinutes(null);
        assertEquals(1, plans.updateDefinition(saved));
        StudyPlanEntity kept = plans.selectById(plan.getId());
        assertNotNull(kept.getEndDate(), "end_date 传 null 被清空了，NOT_NULL 更新语义在迁移中丢了");
        assertEquals(60, kept.getDailyTargetMinutes().intValue(),
                "daily_target_minutes 传 null 被清空了，NOT_NULL 更新语义在迁移中丢了");

        assertEquals(1, plans.updateStatus(plan.getId(), "ACTIVE", 1));
        StudyPlanEntity active = plans.selectById(plan.getId());
        assertEquals("ACTIVE", active.getStatus());
        assertEquals(1, active.getGeneratedVersion().intValue());
        assertEquals(1, plans.countActiveByAccountId(accountId));
    }

    @Test
    void studyTaskMapperFollowsItsStateGuardsAndPlanRecompute() {
        Long planId = probeId();
        Long accountId = probeId();
        Long tutorialId = probeId();
        Long chapterId = probeId();
        LocalDate today = LocalDate.now();

        assertEquals(1, tasks.insertPlanned(planId, accountId, tutorialId, chapterId, today, 1, 1));
        assertEquals(0, tasks.insertPlanned(planId, accountId, tutorialId, chapterId, today, 1, 1),
                "唯一键重复时 INSERT IGNORE 应返回 0，计划重算必须幂等");

        List<StudyTaskEntity> rows = tasks.listByPlanId(planId);
        assertEquals(1, rows.size());
        StudyTaskEntity first = rows.get(0);
        assertEquals("TODO", first.getStatus());
        assertEquals(1, first.getSequenceNo().intValue());
        assertEquals("CHAPTER", first.getTargetType());
        assertEquals(today, first.getTaskDate());
        assertNotNull(tasks.selectById(first.getId()));
        assertEquals(1, tasks.listDueByAccountId(accountId, today).size());
        assertTrue(tasks.listCompletedByPlanId(planId).isEmpty());

        assertEquals(1, tasks.start(first.getId(), accountId));
        assertEquals(0, tasks.start(first.getId(), accountId), "状态守卫应拒绝重复开始");
        assertEquals(1, tasks.complete(first.getId(), accountId));
        assertEquals(0, tasks.complete(first.getId(), accountId), "已完成任务不能重复完成");
        assertEquals(0, tasks.skip(first.getId(), accountId), "已完成任务不能跳过");
        assertEquals(1, tasks.listCompletedByPlanId(planId).size());
        assertTrue(tasks.listDueByAccountId(accountId, today).isEmpty(), "已完成任务不应留在今日列表");

        // 逾期 + 结束计划时整体跳过
        assertEquals(1, tasks.insertPlanned(planId, accountId, tutorialId, chapterId + 1, today.minusDays(1), 2, 1));
        assertEquals(1, tasks.markOverdue(accountId, today));
        StudyTaskEntity overdue = tasks.listByPlanId(planId).stream()
                .filter(item -> !item.getId().equals(first.getId())).findFirst().orElseThrow();
        assertEquals("OVERDUE", tasks.selectById(overdue.getId()).getStatus());
        assertEquals(1, tasks.skipUnfinishedByPlanId(planId));
        assertEquals("SKIPPED", tasks.selectById(overdue.getId()).getStatus());

        // 计划重算：删除未完成任务，保留已完成任务
        assertEquals(1, tasks.insertPlanned(planId, accountId, tutorialId, chapterId + 2, today, 3, 2));
        assertTrue(tasks.deleteUnfinishedByPlanId(planId) >= 1);
        assertNotNull(tasks.selectById(first.getId()), "重算不能删除已完成任务");
        assertEquals(1, tasks.listByPlanId(planId).size());
    }

    @Test
    void learningProgressMapperUpsertsAccumulatesAndCompletesOnce() {
        Long accountId = probeId();
        Long tutorialId = probeId();
        Long groupId = probeId();
        Long chapterId = probeId();

        assertEquals(1, progress.ensureRow(accountId, tutorialId, groupId, chapterId));
        assertEquals(0, progress.ensureRow(accountId, tutorialId, groupId, chapterId),
                "同一账户同一章节只应有一行");
        LearningProgressEntity created = progress.selectByAccountIdAndChapterId(accountId, chapterId);
        assertNotNull(created);
        assertEquals(0L, created.getStudySecondsTotal().longValue());
        assertNull(created.getCompletedAt());
        assertNotNull(created.getLastStudiedAt());

        // INSERT ... ON DUPLICATE KEY UPDATE 命中已有行并真正修改时，MySQL 返回 2
        assertEquals(2, progress.upsertReading(accountId, tutorialId, groupId, chapterId,
                "anchor-1", new BigDecimal("0.5000"), 30));
        LearningProgressEntity after = progress.selectByAccountIdAndChapterId(accountId, chapterId);
        assertEquals(30L, after.getStudySecondsTotal().longValue(), "学习时长应在 UPSERT 中累加");
        assertEquals("anchor-1", after.getScrollAnchor());
        assertEquals(0, new BigDecimal("0.5000").compareTo(after.getProgressRatio()));

        // anchor 允许为空：null 也必须写进 UPDATE
        assertEquals(2, progress.upsertReading(accountId, tutorialId, groupId, chapterId,
                null, new BigDecimal("0.7500"), 10));
        LearningProgressEntity second = progress.selectByAccountIdAndChapterId(accountId, chapterId);
        assertEquals(40L, second.getStudySecondsTotal().longValue());
        assertNull(second.getScrollAnchor(), "scroll_anchor 传 null 没有覆盖旧值");
        assertEquals(0, new BigDecimal("0.7500").compareTo(second.getProgressRatio()));

        assertEquals(1, progress.markCompleted(accountId, chapterId));
        assertEquals(0, progress.markCompleted(accountId, chapterId), "完成事件只应触发一次");
        assertNotNull(progress.selectByAccountIdAndChapterId(accountId, chapterId).getCompletedAt());

        assertEquals(1, progress.listByAccountId(accountId).size());
        assertEquals(1, progress.listByAccountIdAndTutorialId(accountId, tutorialId).size());
        assertEquals(1, progress.listRecentByAccountId(accountId).size());
    }

    @Test
    void learningHistoryMapperAppendsAndPagesNewestFirst() {
        Long accountId = probeId();

        LearningHistoryEntity first = new LearningHistoryEntity();
        first.setAccountId(accountId);
        first.setEventType("CHAPTER_OPENED");
        first.setTutorialId(probeId());
        first.setChapterId(probeId());
        assertEquals(1, history.insert(first));
        assertNotNull(first.getId(), "insert 没有回填自增主键");

        LearningHistoryEntity second = new LearningHistoryEntity();
        second.setAccountId(accountId);
        second.setEventType("REVIEW_COMPLETED");
        second.setTutorialId(first.getTutorialId());
        second.setKnowledgeCardId(probeId());
        second.setDetailJson("{\"rating\":\"NORMAL\"}");
        assertEquals(1, history.insert(second));

        assertEquals(2, history.countByAccountId(accountId));
        List<LearningHistoryEntity> page = history.pageByAccountId(accountId, 1, 0);
        assertEquals(1, page.size(), "LIMIT/OFFSET 没有生效");
        assertEquals(second.getId(), page.get(0).getId(), "历史应按 occurred_at 倒序，同一毫秒按 id 倒序");
        assertNotNull(page.get(0).getOccurredAt(), "occurred_at 由列默认值填充，应能读回");
        // MySQL 的 JSON 列会规范化空白，这里只断言内容而不是字面量
        assertTrue(page.get(0).getDetailJson().contains("NORMAL"));

        List<LearningHistoryEntity> secondPage = history.pageByAccountId(accountId, 1, 1);
        assertEquals(first.getId(), secondPage.get(0).getId());
        assertNull(secondPage.get(0).getDetailJson(), "detail_json 为 null 的事件也要能读回");
    }

    @Test
    void knowledgeMasteryMapperEnsuresOnceAndUpdatesEvidence() {
        Long accountId = probeId();
        Long cardId = probeId();

        assertEquals(1, mastery.ensure(accountId, cardId));
        assertEquals(0, mastery.ensure(accountId, cardId), "同一账户同一卡片只应有一行");
        KnowledgeMasteryEntity row = mastery.selectByAccountIdAndCardId(accountId, cardId);
        assertNotNull(row);
        assertEquals("L1", row.getSystemSuggestedLevel());
        assertEquals(0, row.getEvidenceCount().intValue());
        assertNull(row.getFirstLearnedAt());
        assertEquals(1, mastery.listByAccountId(accountId).size());

        Long learnedCardId = probeId();
        assertEquals(1, mastery.ensureLearned(accountId, learnedCardId));
        assertNotNull(mastery.selectByAccountIdAndCardId(accountId, learnedCardId).getFirstLearnedAt(),
                "ensureLearned 必须写入首次学习时间");
        LocalDateTime firstLearnedAt = mastery.selectByAccountIdAndCardId(accountId, learnedCardId).getFirstLearnedAt();
        assertEquals(1, mastery.markLearned(accountId, learnedCardId));
        assertEquals(firstLearnedAt, mastery.selectByAccountIdAndCardId(accountId, learnedCardId).getFirstLearnedAt(),
                "markLearned 不能覆盖已有的首次学习时间");
        assertEquals(2, mastery.listByAccountId(accountId).size());

        LocalDateTime at = now();
        assertEquals(1, mastery.updateEvidence(row.getId(), 3, "EASY", at, "L2"));
        KnowledgeMasteryEntity evidence = mastery.selectByAccountIdAndCardId(accountId, cardId);
        assertEquals(3, evidence.getEvidenceCount().intValue());
        assertEquals("EASY", evidence.getLastRecallRating());
        assertEquals("L2", evidence.getSystemSuggestedLevel());
        assertNotNull(evidence.getLastEvidenceAt());

        assertEquals(1, mastery.updateSelfLevel(row.getId(), "L3"));
        KnowledgeMasteryEntity self = mastery.selectByAccountIdAndCardId(accountId, cardId);
        assertEquals("L3", self.getUserSelfLevel());
        assertEquals(3, self.getEvidenceCount().intValue(), "自评不应影响证据统计");
    }

    @Test
    void reviewTaskMapperFollowsDueAndStatusGuards() {
        Long accountId = probeId();
        Long scheduleId = probeId();
        Long cardId = probeId();
        LocalDateTime dueAt = now().minusMinutes(1);

        assertEquals(1, reviewTasks.insertDue(scheduleId, accountId, cardId, dueAt));
        assertEquals(0, reviewTasks.insertDue(scheduleId, accountId, cardId, dueAt),
                "同一 schedule_id + due_at 只应有一条任务");

        List<ReviewTaskEntity> pending = reviewTasks.listPendingByAccountId(accountId);
        assertEquals(1, pending.size());
        ReviewTaskEntity task = pending.get(0);
        assertEquals("PENDING", task.getStatus());
        assertEquals(cardId, task.getKnowledgeCardId());
        assertNotNull(reviewTasks.selectById(task.getId()));
        assertNotNull(task.getGeneratedAt(), "generated_at 由列默认值填充，应能读回");
        assertEquals(0, reviewTasks.countCompletedByAccountId(accountId));

        assertEquals(1, reviewTasks.markOverdue(accountId, now()));
        assertEquals("OVERDUE", reviewTasks.selectById(task.getId()).getStatus());
        assertEquals(1, reviewTasks.listPendingByAccountId(accountId).size(), "OVERDUE 仍属于待复习");

        assertEquals(1, reviewTasks.complete(task.getId(), accountId));
        assertEquals(0, reviewTasks.complete(task.getId(), accountId), "已完成任务不能重复完成");
        assertEquals("COMPLETED", reviewTasks.selectById(task.getId()).getStatus());
        assertEquals(1, reviewTasks.countCompletedByAccountId(accountId));
        assertTrue(reviewTasks.listPendingByAccountId(accountId).isEmpty());
    }

    @Test
    void reviewScheduleMapperQueuesDueRowsOnlyOnce() {
        Long accountId = probeId();
        Long cardId = probeId();
        LocalDateTime now = now();

        assertEquals(1, schedules.ensure(accountId, cardId, 1, now.minusMinutes(1)));
        assertEquals(0, schedules.ensure(accountId, cardId, 1, now.minusMinutes(1)),
                "同一账户同一卡片只应有一条复习计划");

        List<ReviewScheduleEntity> due = schedules.dueUnqueuedForAccount(accountId, now);
        assertEquals(1, due.size(), "已到期且未入队的计划应被查到");
        ReviewScheduleEntity schedule = due.get(0);
        assertEquals(cardId, schedule.getKnowledgeCardId());
        assertEquals("ACTIVE", schedule.getStatus());
        assertEquals(1, schedule.getCurrentIntervalDays().intValue());
        assertEquals(0, schedule.getStepIndex().intValue());
        assertNotNull(schedules.selectById(schedule.getId()));
        assertNotNull(schedules.dueUnqueued(now), "全库入队扫描必须能执行（NOT EXISTS 子查询 + LIMIT）");

        assertEquals(1, reviewTasks.insertDue(schedule.getId(), accountId, cardId, schedule.getNextReviewAt()));
        assertTrue(schedules.dueUnqueuedForAccount(accountId, now).isEmpty(), "已入队的计划不应重复入队");
        assertFalse(schedules.dueUnqueued(now).stream().anyMatch(item -> item.getId().equals(schedule.getId())),
                "已入队的计划不应出现在全库扫描结果里");

        assertEquals(1, schedules.updateAfterReview(schedule.getId(), 2, 6, now.plusDays(6), now));
        ReviewScheduleEntity advanced = schedules.selectById(schedule.getId());
        assertEquals(2, advanced.getStepIndex().intValue());
        assertEquals(6, advanced.getCurrentIntervalDays().intValue());
        assertNotNull(advanced.getLastResultAt());
    }

    @Test
    void reviewResultMapperCountsOnlyGoodRatings() {
        Long accountId = probeId();
        Long cardId = probeId();

        ReviewResultEntity forgot = newReviewResult(accountId, cardId, "FORGOT");
        assertEquals(1, reviewResults.insert(forgot));
        assertNotNull(forgot.getId(), "insert 没有回填自增主键");
        assertEquals(0, reviewResults.countGoodByAccountIdAndCardId(accountId, cardId));

        ReviewResultEntity easy = newReviewResult(accountId, cardId, "EASY");
        assertEquals(1, reviewResults.insert(easy));
        assertEquals(1, reviewResults.countGoodByAccountIdAndCardId(accountId, cardId));

        ReviewResultEntity normal = newReviewResult(accountId, cardId, "NORMAL");
        assertEquals(1, reviewResults.insert(normal));
        assertEquals(2, reviewResults.countGoodByAccountIdAndCardId(accountId, cardId));
        assertEquals(0, reviewResults.countGoodByAccountIdAndCardId(probeId(), cardId),
                "只应统计当前账户自己的复习结果");
    }

    @Test
    void userQuestionAnswerMapperUpsertsWithoutLosingFirstSubmissionTime() {
        Long accountId = probeId();
        Long questionId = probeId();
        assertNull(answers.selectByAccountIdAndQuestionId(accountId, questionId));

        assertEquals(1, answers.upsertAnswer(accountId, questionId, "第一次答案"));
        UserQuestionAnswerEntity first = answers.selectByAccountIdAndQuestionId(accountId, questionId);
        assertNotNull(first);
        assertEquals("第一次答案", first.getAnswerText());
        assertNotNull(first.getFirstSubmittedAt(), "first_submitted_at 由列默认值填充，应能读回");
        assertNull(first.getReferenceUnlockedAt(), "V2_028 后旧覆盖式 Mapper 不会解锁参考答案；解锁由答案版本业务完成");

        assertEquals(2, answers.upsertAnswer(accountId, questionId, "第二次答案"),
                "ON DUPLICATE KEY UPDATE 更新已有行时返回 2");
        UserQuestionAnswerEntity updated = answers.selectByAccountIdAndQuestionId(accountId, questionId);
        assertEquals("第二次答案", updated.getAnswerText());
        assertEquals(first.getFirstSubmittedAt(), updated.getFirstSubmittedAt(), "首次提交时间不能被覆盖");
        assertEquals(first.getId(), updated.getId(), "同一账户同一题目只应有一行");
    }

    @Test
    void rollbackLeavesNoTestRowsInTheDevelopmentDatabase() {
        TutorialCategoryEntity row = newCategory(unique("mapper-xml-test-rollback"));
        assertEquals(1, categories.insert(row));
        Long id = row.getId();
        assertNotNull(id);

        session.rollback();
        try (SqlSession fresh = openSession()) {
            assertNull(fresh.getMapper(TutorialCategoryMapper.class).selectById(id),
                    "回滚后不应在开发库里留下测试数据");
        }
    }

    private TutorialEntity insertTutorial() {
        TutorialCategoryEntity category = newCategory(unique("mapper-xml-test-category"));
        categories.insert(category);
        TutorialEntity tutorial = newTutorial(category.getId(), unique("mapper-xml-test-tutorial"));
        tutorials.insert(tutorial);
        return tutorial;
    }

    private TutorialChapterEntity insertChapter() {
        TutorialEntity tutorial = insertTutorial();
        TutorialGroupEntity group = newGroup(tutorial.getId(), "分组");
        groups.insert(group);
        TutorialChapterEntity chapter = newChapter(tutorial.getId(), group.getId(), unique("mapper-xml-test-chapter"));
        chapters.insert(chapter);
        return chapter;
    }

    private TutorialCategoryEntity newCategory(String slug) {
        TutorialCategoryEntity row = new TutorialCategoryEntity();
        row.setName("XML 验证分类");
        row.setSlug(slug);
        row.setSortOrder(10);
        return row;
    }

    private TutorialEntity newTutorial(Long categoryId, String slug) {
        TutorialEntity row = new TutorialEntity();
        row.setCategoryId(categoryId);
        row.setSlug(slug);
        row.setTitle("XML 验证教程");
        row.setSummary("摘要");
        row.setSortOrder(10);
        row.setPublicationStatus("NEVER_PUBLISHED");
        row.setEditingStatus("DRAFT");
        row.setCreatedByAccountId(probeId());
        row.setUpdatedByAccountId(row.getCreatedByAccountId());
        return row;
    }

    private TutorialGroupEntity newGroup(Long tutorialId, String title) {
        TutorialGroupEntity row = new TutorialGroupEntity();
        row.setTutorialId(tutorialId);
        row.setTitle(title);
        row.setSortOrder(10);
        row.setStatus("ACTIVE");
        return row;
    }

    private TutorialChapterEntity newChapter(Long tutorialId, Long groupId, String slug) {
        TutorialChapterEntity row = new TutorialChapterEntity();
        row.setTutorialId(tutorialId);
        row.setGroupId(groupId);
        row.setSlug(slug);
        row.setTitle("XML 验证章节");
        row.setBodyMarkdown("# 正文");
        row.setSortOrder(10);
        // V2_026 之后章节状态是 DRAFT/PUBLISHED/WITHDRAWN，countActiveByGroupId 统计的是 PUBLISHED
        row.setStatus("PUBLISHED");
        return row;
    }

    private TutorialRevisionEntity newRevision(Long tutorialId, int revisionNo) {
        TutorialRevisionEntity row = new TutorialRevisionEntity();
        row.setTutorialId(tutorialId);
        row.setRevisionNo(revisionNo);
        row.setRevisionRef("mapper-xml-test:" + tutorialId + ":revision:" + revisionNo + ":" + PROBE.incrementAndGet());
        row.setSnapshotJson("{\"id\":\"" + tutorialId + "\",\"title\":\"快照\"}");
        row.setContentSha256("0".repeat(64));
        row.setCreatedByAccountId(probeId());
        return row;
    }

    private StudyPlanEntity newPlan(Long accountId) {
        StudyPlanEntity row = new StudyPlanEntity();
        row.setAccountId(accountId);
        row.setTutorialId(probeId());
        row.setName("XML 验证计划");
        row.setScopeType("TUTORIAL");
        row.setScopeId(row.getTutorialId());
        row.setStartDate(LocalDate.now());
        row.setStudyWeekdaysJson("[1,2,3,4,5]");
        row.setStatus("DRAFT");
        row.setGeneratedVersion(0);
        return row;
    }

    private ReviewResultEntity newReviewResult(Long accountId, Long cardId, String rating) {
        ReviewResultEntity row = new ReviewResultEntity();
        row.setReviewTaskId(probeId());
        row.setScheduleId(probeId());
        row.setAccountId(accountId);
        row.setKnowledgeCardId(cardId);
        row.setRecallRating(rating);
        row.setPreviousIntervalDays(1);
        row.setNextIntervalDays(6);
        row.setPreviousNextReviewAt(now().minusDays(1));
        row.setCalculatedNextReviewAt(now().plusDays(6));
        return row;
    }

    /* 只做逻辑外键，库里没有物理 FOREIGN KEY；用一个大号段避免和真实账户/教程撞号 */
    private long probeId() {
        return 900_000_000_000L + PROBE.incrementAndGet();
    }

    private String unique(String prefix) {
        return prefix + "-" + PROBE.incrementAndGet() + "-" + System.nanoTime();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
    }
}
