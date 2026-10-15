/*
 * TutorialWorkspaceIntegrationTest：应用启动模块的测试文件。
 * 业务范围：多模块装配、入口路由、安全配置和启动初始化。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：CurrentActorApi、GlobalApiExceptionHandler、ReviewSubmissionApi、TutorialStructureAdminController、TutorialEventPublisher、TutorialMediaReferences。
 * 阅读入口：setup（setup处理入口）；mapper（数据映射处理入口）；proxy（proxy处理入口）；change（change处理入口）。
 */

package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.handler.GlobalApiExceptionHandler;
import com.starrainnotes.review.api.ReviewSubmissionApi;
import com.starrainnotes.tutorial.content.controller.TutorialStructureAdminController;
import com.starrainnotes.tutorial.content.dto.*;
import com.starrainnotes.tutorial.content.entity.*;
import com.starrainnotes.tutorial.content.event.TutorialEventPublisher;
import com.starrainnotes.tutorial.content.mapper.*;
import com.starrainnotes.tutorial.content.media.TutorialMediaReferences;
import com.starrainnotes.tutorial.content.service.*;
import com.starrainnotes.tutorial.content.service.impl.*;
import java.sql.*;
import java.util.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Actual Flyway upgrade, MyBatis SQL and Spring transactions in a disposable local schema. */
class TutorialWorkspaceIntegrationTest extends MapperXmlIntegrationSupport {
    /**
     * connectionOwner。
     */
    private SqlSession connectionOwner;
    /**
     * connection。
     */
    private Connection connection;
    /**
     * 原始。
     */
    private String original, schema;
    /**
     * data来源。
     */
    private SingleConnectionDataSource dataSource;
    /**
     * sql。
     */
    private SqlSessionTemplate sql;
    /**
     * transactions。
     */
    private DataSourceTransactionManager transactions;
    /**
     * 内容。
     * 协作类型：<code>TutorialContentService</code>。
     */
    private TutorialContentService content;
    /**
     * 发布。
     * 协作类型：<code>TutorialPublicationService</code>。
     */
    private TutorialPublicationService publication;
    /**
     * operations。
     * 协作类型：<code>TutorialOperationService</code>。
     */
    private TutorialOperationService operations;
    /**
     * actors。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private CurrentActorApi actors;
    /**
     * 媒体。
     */
    private TutorialMediaReferences media;
    /**
     * 教程。
     */
    private TutorialEntity tutorial;
    /**
     * 章节。
     */
    private TutorialChapterEntity chapter;
    /**
     * JSON 读写或映射协作对象。
     * 协作类型：<code>ObjectMapper</code>。
     */
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    /**
     * setup处理入口。
     * 主要协作调用：<code>connectionOwner.getConnection</code>、<code>connection.getCatalog</code>、<code>schema.matches</code>、<code>connection.createStatement</code>、<code>connection.setCatalog</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @BeforeEach void setup() throws Exception {
        connectionOwner = openSession(); connection = connectionOwner.getConnection(); original = connection.getCatalog();
        schema = "sr_tutorial_workspace_probe_" + Long.toUnsignedString(System.nanoTime());
        assertTrue(schema.matches("sr_tutorial_workspace_probe_[0-9]+"));
        try (Statement statement = connection.createStatement()) { statement.execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4"); }
        connection.setCatalog(schema);connection.setAutoCommit(true);
        dataSource = new SingleConnectionDataSource(connection,true);
        Flyway.configure().dataSource(dataSource).defaultSchema(schema).schemas(schema).target("2.037").load().migrate();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO sr_tutorial_category(name,slug,sort_order) VALUES('Before upgrade','before-upgrade',0)");
        }
        Flyway upgraded = Flyway.configure().dataSource(dataSource).defaultSchema(schema).schemas(schema).load();
        upgraded.migrate(); upgraded.validate(); assertEquals("2.047",upgraded.info().current().getVersion().getVersion());
        try (Statement statement=connection.createStatement(); ResultSet row=statement.executeQuery("SELECT COUNT(*) FROM sr_tutorial_category WHERE slug='before-upgrade'")) { assertTrue(row.next());assertEquals(1,row.getInt(1)); }
        Configuration configuration = new Configuration(); configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment("workspace-test",new SpringManagedTransactionFactory(),dataSource));
        for (String resource : xmlResources()) {
            try (var in=org.apache.ibatis.io.Resources.getResourceAsStream(resource)) {
                new XMLMapperBuilder(in,configuration,resource,configuration.getSqlFragments()).parse();
            }
        }
        sql=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(configuration));
        transactions=new DataSourceTransactionManager(dataSource);
        actors=mock(CurrentActorApi.class);when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(71L).roles(Set.of("SUPER_ADMIN")).build());
        media=mock(TutorialMediaReferences.class);
        content=proxy(new TutorialContentServiceImpl(mapper(TutorialCategoryMapper.class),mapper(TutorialMapper.class),mapper(TutorialGroupMapper.class),
                mapper(TutorialChapterMapper.class),actors,media,mock(TutorialDependencyCleanup.class),mapper(TutorialRevisionMapper.class)),TutorialContentService.class);
        publication=proxy(new TutorialPublicationServiceImpl(mapper(TutorialMapper.class),mapper(TutorialCategoryMapper.class),mapper(TutorialGroupMapper.class),
                mapper(TutorialChapterMapper.class),mapper(TutorialKnowledgeCardMapper.class),mapper(TutorialQuestionMapper.class),mapper(TutorialQuestionCardMapper.class),
                mapper(TutorialRevisionMapper.class),content,actors,json,mock(TutorialEventPublisher.class),media,mock(ReviewSubmissionApi.class)),TutorialPublicationService.class);
        var reviews=mock(ReviewSubmissionApi.class);
        when(reviews.submit(any())).thenReturn(com.starrainnotes.review.api.dto.ReviewSubmissionResult.builder().reviewRequestId(1L).status("PENDING").build());
        operations=proxy(new TutorialOperationService(content,mock(TutorialExerciseService.class),publication,
                mapper(TutorialOperationMapper.class),mapper(TutorialMapper.class),mapper(TutorialGroupMapper.class),mapper(TutorialChapterMapper.class),
                mapper(TutorialKnowledgeCardMapper.class),mapper(TutorialQuestionMapper.class),actors,reviews,json),TutorialOperationService.class);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var category=new TutorialCategoryEntity();category.setName("Java");category.setSlug("java");category.setSortOrder(0);mapper(TutorialCategoryMapper.class).insert(category);
            tutorial=new TutorialEntity();tutorial.setCategoryId(category.getId());tutorial.setTitle("Tutorial");tutorial.setSlug("java");tutorial.setSummary("Summary");tutorial.setSortOrder(0);
            tutorial.setPublicationStatus("NEVER_PUBLISHED");tutorial.setEditingStatus("DRAFT");tutorial.setCreatedByAccountId(71L);tutorial.setUpdatedByAccountId(71L);mapper(TutorialMapper.class).insert(tutorial);
            var group=new TutorialGroupEntity();group.setTutorialId(tutorial.getId());group.setTitle("Group");group.setStatus("ACTIVE");group.setSortOrder(0);mapper(TutorialGroupMapper.class).insert(group);
            chapter=new TutorialChapterEntity();chapter.setTutorialId(tutorial.getId());chapter.setGroupId(group.getId());chapter.setTitle("Original title");chapter.setSummary("Original summary");
            chapter.setSlug("intro");chapter.setBodyMarkdown("Original body");chapter.setStatus("DRAFT");chapter.setSortOrder(0);mapper(TutorialChapterMapper.class).insert(chapter);
        });
        publication.publish(tutorial.getId());
    }
    /**
     * 数据映射处理入口。
     * 主要协作调用：<code>sql.getMapper</code>。
     *
     * @param type 类型；类型为 <code>Class&lt;T&gt;</code>
     * @return 类型为 <code>T</code> 的处理结果
     */
    private <T> T mapper(Class<T> type) { return sql.getMapper(type); }
    /**
     * proxy处理入口。
     * 关键处理步骤调用 <code>factory.addAdvice</code>、<code>type.cast</code>、<code>factory.getProxy</code>。
     *
     * @param target 目标；类型为 <code>T</code>
     * @param type 类型；类型为 <code>Class&lt;T&gt;</code>
     * @return 类型为 <code>T</code> 的处理结果
     */
    private <T> T proxy(T target,Class<T> type) {
        ProxyFactory factory=new ProxyFactory(target);
        factory.addAdvice(new TransactionInterceptor(transactions,new AnnotationTransactionAttributeSource()));
        return type.cast(factory.getProxy());
    }
    /**
     * change处理入口。
     * 关键处理步骤调用 <code>ChapterUpdateDTO.builder</code>。
     *
     * @param version 版本；类型为 <code>Long</code>
     * @return 类型为 <code>ChapterUpdateDTO</code> 的处理结果
     */
    private ChapterUpdateDTO change(Long version) { return ChapterUpdateDTO.builder().title("Edited title").summary("Edited summary").bodyMarkdown("Edited body").rowVersion(version).build(); }
    /**
     * 清理教程WorkspaceIntegration。
     * 主要协作调用：<code>connection.getAutoCommit</code>、<code>connection.rollback</code>、<code>connection.setAutoCommit</code>、<code>connection.setCatalog</code>、<code>schema.matches</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @AfterEach void cleanup() throws Exception {
        if (connection != null) {
            if (!connection.getAutoCommit()) connection.rollback();
            connection.setAutoCommit(true);connection.setCatalog(original);
            if (schema != null && schema.matches("sr_tutorial_workspace_probe_[0-9]+")) {
                try (Statement statement=connection.createStatement()) { statement.execute("DROP DATABASE " + schema); }
            }
        }
        if (connectionOwner != null) connectionOwner.close();
    }
    /**
     * workspaceSave是否原子操作公开修订StaysFrozen及Stale客户端GetsHTTP409处理入口。
     * 主要协作调用：<code>content.chapter</code>、<code>chapter.getId</code>、<code>content.updateChapter</code>、<code>publication.publicChapter</code>、<code>json.writeValueAsString</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void workspaceSaveIsAtomicPublicRevisionStaysFrozenAndStaleClientGetsHttp409() throws Exception {
        var originalChapter=content.chapter(chapter.getId());
        var saved=content.updateChapter(chapter.getId(),change(originalChapter.getRowVersion()));
        assertEquals("Edited title",saved.getTitle());assertEquals("Edited summary",saved.getSummary());assertEquals("Edited body",saved.getBodyMarkdown());
        assertEquals(originalChapter.getRowVersion()+1,saved.getRowVersion());
        assertEquals("Original body",publication.publicChapter("java","intro").path("bodyMarkdown").asText());
        var mvc=MockMvcBuilders.standaloneSetup(new TutorialStructureAdminController(operations,content,publication)).setControllerAdvice(new GlobalApiExceptionHandler()).build();
        mvc.perform(patch("/api/admin/tutorial-chapters/"+chapter.getId()).contentType("application/json").content(json.writeValueAsString(change(originalChapter.getRowVersion()))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TUTORIAL_CONFLICT"));
        publication.publishChapter(chapter.getId());
        assertEquals("Edited body",publication.publicChapter("java","intro").path("bodyMarkdown").asText());
    }
    /**
     * 后台EditLeavesWorkspace及公开内容UnchangedUntilApproval及ReplaysOnlyOnce处理入口。
     * 主要协作调用：<code>actors.current</code>、<code>content.chapter</code>、<code>chapter.getId</code>、<code>operations.request</code>、<code>publication.publicChapter</code>。
     */
    @Test void adminEditLeavesWorkspaceAndPublicContentUnchangedUntilApprovalAndReplaysOnlyOnce() {
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(72L).roles(Set.of("ADMIN")).build());
        var before=content.chapter(chapter.getId());
        var response=(Map<?,?>)operations.request("updateChapter",new Object[]{chapter.getId(),change(before.getRowVersion())});
        assertEquals(true,response.get("reviewPending"));
        assertEquals("Original body",content.chapter(chapter.getId()).getBodyMarkdown());
        assertEquals("Original body",publication.publicChapter("java","intro").path("bodyMarkdown").asText());
        var pending=mapper(TutorialOperationMapper.class).pending("tutorial:"+tutorial.getId());
        assertNotNull(pending);
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(71L).roles(Set.of("SUPER_ADMIN")).build());
        operations.approve(pending.getId());
        assertEquals("Edited body",content.chapter(chapter.getId()).getBodyMarkdown());
        assertEquals("Edited body",publication.publicChapter("java","intro").path("bodyMarkdown").asText());
        assertNull(mapper(TutorialOperationMapper.class).pending("tutorial:"+tutorial.getId()));
        assertEquals("APPLIED",mapper(TutorialOperationMapper.class).selectById(pending.getId()).getStatus());
        var appliedVersion=content.chapter(chapter.getId()).getRowVersion();
        operations.approve(pending.getId());
        assertEquals(appliedVersion,content.chapter(chapter.getId()).getRowVersion());
    }
    /**
     * committedApprovalSurvivesCallback失败及显式Recovery是否Idempotent处理入口。
     * 主要协作调用：<code>actors.current</code>、<code>operations.request</code>、<code>chapter.getId</code>、<code>content.chapter</code>、<code>tutorial.getId</code>。
     */
    @Test void committedApprovalSurvivesCallbackFailureAndExplicitRecoveryIsIdempotent() {
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(72L).roles(Set.of("ADMIN")).build());
        operations.request("updateChapter", new Object[]{chapter.getId(), change(content.chapter(chapter.getId()).getRowVersion())});
        var pending = mapper(TutorialOperationMapper.class).pending("tutorial:" + tutorial.getId());
        var requestMapper = mapper(com.starrainnotes.review.mapper.ReviewRequestMapper.class);
        var request = new com.starrainnotes.review.entity.ReviewRequestEntity();
        request.setReviewType("tutorial.operation"); request.setTargetModule("TUTORIAL"); request.setTargetType("OPERATION");
        request.setTargetId(pending.getId()); request.setTargetRevisionRef("tutorial-operation:" + pending.getId());
        request.setTargetDisplayName("Update chapter"); request.setApplicantAccountId(72L); request.setApplicantDisplayName("Isolated administrator");
        request.setStatus("PENDING"); request.setSubmittedAt(java.time.LocalDateTime.now()); requestMapper.insertRequest(request);
        var handler = new com.starrainnotes.tutorial.content.handler.TutorialOperationReviewHandler(mapper(TutorialOperationMapper.class), operations);
        var registry = mock(com.starrainnotes.review.handler.ReviewTargetHandlerRegistry.class);
        when(registry.require("TUTORIAL", "OPERATION", "tutorial.operation")).thenReturn(handler);
        var decisions = proxy(new com.starrainnotes.review.service.impl.ReviewDecisionServiceImpl(requestMapper,
                mapper(com.starrainnotes.review.mapper.ReviewActionMapper.class), registry,
                new com.starrainnotes.review.handler.ReviewDecisionCallbackDispatcher()), com.starrainnotes.review.service.ReviewDecisionService.class);
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(71L).roles(Set.of("SUPER_ADMIN")).build());
        doThrow(new IllegalStateException("SQL internal detail must not reach the administrator")).when(media).replaceChapter(eq(chapter.getId()), anyString());
        assertThrows(IllegalStateException.class, () -> decisions.approve(request.getId(), null, 71L));
        assertEquals("APPROVED", requestMapper.requestById(request.getId()).getStatus());
        var failed = mapper(TutorialOperationMapper.class).selectById(pending.getId());
        assertEquals("FAILED", failed.getStatus()); assertFalse(failed.getExecutionMessage().contains("SQL"));
        assertEquals("Original body", content.chapter(chapter.getId()).getBodyMarkdown());
        reset(media);
        decisions.recoverApproved(request.getId(), 71L);
        assertEquals("Edited body", content.chapter(chapter.getId()).getBodyMarkdown());
        Long version = content.chapter(chapter.getId()).getRowVersion();
        decisions.recoverApproved(request.getId(), 71L);
        assertEquals(version, content.chapter(chapter.getId()).getRowVersion());
        assertEquals("APPLIED", mapper(TutorialOperationMapper.class).selectById(pending.getId()).getStatus());
        assertNull(mapper(TutorialOperationMapper.class).selectById(pending.getId()).getExecutionMessage());
    }

    /**
     * recovers已批准待处理Crash时间窗口OnlyWhile原始BaselineStillMatches处理入口。
     * 主要协作调用：<code>actors.current</code>、<code>operations.request</code>、<code>chapter.getId</code>、<code>content.chapter</code>、<code>tutorial.getId</code>。
     */
    @Test void recoversApprovedPendingCrashWindowOnlyWhileOriginalBaselineStillMatches() {
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(72L).roles(Set.of("ADMIN")).build());
        operations.request("updateChapter", new Object[]{chapter.getId(), change(content.chapter(chapter.getId()).getRowVersion())});
        var pending = mapper(TutorialOperationMapper.class).pending("tutorial:" + tutorial.getId());
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(71L).roles(Set.of("SUPER_ADMIN")).build());
        operations.recoverApproved(pending.getId());
        assertEquals("Edited body", content.chapter(chapter.getId()).getBodyMarkdown());
        assertEquals("APPLIED", mapper(TutorialOperationMapper.class).selectById(pending.getId()).getStatus());
    }

    /**
     * recovery拒绝ChangedBaseline及NeverReplays已拒绝或CanceledOperations处理入口。
     * 主要协作调用：<code>actors.current</code>、<code>operations.request</code>、<code>chapter.getId</code>、<code>content.chapter</code>、<code>tutorial.getId</code>。
     */
    @Test void recoveryRejectsChangedBaselineAndNeverReplaysRejectedOrCanceledOperations() {
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(72L).roles(Set.of("ADMIN")).build());
        operations.request("updateChapter", new Object[]{chapter.getId(), change(content.chapter(chapter.getId()).getRowVersion())});
        var pending = mapper(TutorialOperationMapper.class).pending("tutorial:" + tutorial.getId());
        operations.recordFailure(pending.getId(), new IllegalStateException("interrupted"));
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(71L).roles(Set.of("SUPER_ADMIN")).build());
        var newer = change(content.chapter(chapter.getId()).getRowVersion()); newer.setBodyMarkdown("Newer independent edit");
        content.updateChapter(chapter.getId(), newer);
        Long version = content.chapter(chapter.getId()).getRowVersion();
        assertThrows(com.starrainnotes.tutorial.content.exception.TutorialStateException.class, () -> operations.recoverApproved(pending.getId()));
        assertEquals("Newer independent edit", content.chapter(chapter.getId()).getBodyMarkdown());
        assertEquals(version, content.chapter(chapter.getId()).getRowVersion());
        assertEquals("FAILED", mapper(TutorialOperationMapper.class).selectById(pending.getId()).getStatus());
        for (String terminal : List.of("REJECTED", "CANCELED")) {
            when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(72L).roles(Set.of("ADMIN")).build());
            operations.request("updateChapter", new Object[]{chapter.getId(), change(version)});
            var stopped = mapper(TutorialOperationMapper.class).pending("tutorial:" + tutorial.getId());
            operations.finish(stopped.getId(), terminal);
            assertThrows(com.starrainnotes.tutorial.content.exception.TutorialStateException.class, () -> operations.recoverApproved(stopped.getId()));
            assertEquals(terminal, mapper(TutorialOperationMapper.class).selectById(stopped.getId()).getStatus());
            assertEquals(version, content.chapter(chapter.getId()).getRowVersion());
        }
    }

    /**
     * 媒体引用失败RollsBack标题摘要正文及版本内部RealSpring事务处理入口。
     * 主要协作调用：<code>content.chapter</code>、<code>chapter.getId</code>、<code>content.updateChapter</code>。
     */
    @Test void mediaReferenceFailureRollsBackTitleSummaryBodyAndVersionInRealSpringTransaction() {
        var before=content.chapter(chapter.getId());
        doThrow(new IllegalStateException("media failure")).when(media).replaceChapter(eq(chapter.getId()),anyString());
        assertThrows(IllegalStateException.class,()->content.updateChapter(chapter.getId(),change(before.getRowVersion())));
        var after=content.chapter(chapter.getId());
        assertEquals(before.getTitle(),after.getTitle());assertEquals(before.getSummary(),after.getSummary());
        assertEquals(before.getBodyMarkdown(),after.getBodyMarkdown());assertEquals(before.getRowVersion(),after.getRowVersion());
    }
    /**
     * 卡片及问题WorkspaceUseSame审核锁及CannotOverwriteStaleVersions处理入口。
     * 主要协作调用：<code>chapter.getId</code>、<code>publication.publicChapter</code>、<code>publication.submitReview</code>、<code>tutorial.getId</code>、<code>content.updateChapter</code>。
     */
    @Test void cardAndQuestionWorkspaceUseSameReviewLockAndCannotOverwriteStaleVersions() {
        var exercise=proxy(new TutorialExerciseServiceImpl(mapper(TutorialChapterMapper.class),media,mapper(TutorialMapper.class),mapper(TutorialKnowledgeCardMapper.class),
                mapper(TutorialQuestionMapper.class),mapper(TutorialQuestionCardMapper.class)),TutorialExerciseService.class);
        var card=new KnowledgeCardDTO();card.setFrontText("Recall");card.setBackMarkdown("Answer");
        var created=exercise.createCard(chapter.getId(),card);card.setRowVersion((Long)created.get("rowVersion"));
        Long id=Long.valueOf(created.get("id").toString());card.setFrontText("Edited recall");
        var updated=exercise.updateCard(id,card);
        assertEquals(((Number)created.get("contentVersion")).intValue()+1,((Number)updated.get("contentVersion")).intValue());
        assertThrows(com.starrainnotes.tutorial.content.exception.TutorialConflictException.class,()->exercise.updateCard(id,card));
        assertTrue(publication.publicChapter("java","intro").path("cards").isEmpty());
        publication.submitReview(tutorial.getId(),chapter.getId());
        var question=new ChapterQuestionDTO();question.setQuestionText("Why?");question.setReferenceAnswer("Because");
        assertThrows(com.starrainnotes.tutorial.content.exception.TutorialStateException.class,()->exercise.createQuestion(chapter.getId(),question));
        assertThrows(com.starrainnotes.tutorial.content.exception.TutorialStateException.class,()->content.updateChapter(chapter.getId(),change(content.chapter(chapter.getId()).getRowVersion())));
        var revision=mapper(TutorialRevisionMapper.class).selectLatestByTutorialId(tutorial.getId());
        var handler=proxy(new com.starrainnotes.tutorial.content.handler.TutorialReviewTargetHandler(mapper(TutorialMapper.class),mapper(TutorialRevisionMapper.class),
                json,mock(TutorialEventPublisher.class),mapper(TutorialChapterMapper.class)),com.starrainnotes.review.api.ReviewTargetHandler.class);
        var target=com.starrainnotes.review.api.ReviewTargetRef.builder().targetModule("TUTORIAL").targetType("TUTORIAL").targetId(tutorial.getId()).revisionRef(revision.getRevisionRef()).build();
        handler.onApproved(com.starrainnotes.review.api.ReviewDecisionContext.builder().target(target).build());
        assertEquals("DRAFT",mapper(TutorialMapper.class).selectById(tutorial.getId()).getEditingStatus());
        assertEquals("Edited recall",publication.publicChapter("java","intro").path("cards").get(0).path("frontText").asText());
    }
}
