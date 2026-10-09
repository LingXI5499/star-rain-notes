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
    private SqlSession connectionOwner;
    private Connection connection;
    private String original, schema;
    private SingleConnectionDataSource dataSource;
    private SqlSessionTemplate sql;
    private DataSourceTransactionManager transactions;
    private TutorialContentService content;
    private TutorialPublicationService publication;
    private TutorialMediaReferences media;
    private TutorialEntity tutorial;
    private TutorialChapterEntity chapter;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

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
        upgraded.migrate(); upgraded.validate(); assertEquals("2.038",upgraded.info().current().getVersion().getVersion());
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
        var actors=mock(CurrentActorApi.class);when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(71L).build());
        media=mock(TutorialMediaReferences.class);
        content=proxy(new TutorialContentServiceImpl(mapper(TutorialCategoryMapper.class),mapper(TutorialMapper.class),mapper(TutorialGroupMapper.class),
                mapper(TutorialChapterMapper.class),actors,media,mock(TutorialDependencyCleanup.class),mapper(TutorialRevisionMapper.class)),TutorialContentService.class);
        publication=proxy(new TutorialPublicationServiceImpl(mapper(TutorialMapper.class),mapper(TutorialCategoryMapper.class),mapper(TutorialGroupMapper.class),
                mapper(TutorialChapterMapper.class),mapper(TutorialKnowledgeCardMapper.class),mapper(TutorialQuestionMapper.class),mapper(TutorialQuestionCardMapper.class),
                mapper(TutorialRevisionMapper.class),content,actors,json,mock(TutorialEventPublisher.class),media,mock(ReviewSubmissionApi.class)),TutorialPublicationService.class);
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
    private <T> T mapper(Class<T> type) { return sql.getMapper(type); }
    private <T> T proxy(T target,Class<T> type) {
        ProxyFactory factory=new ProxyFactory(target);
        factory.addAdvice(new TransactionInterceptor(transactions,new AnnotationTransactionAttributeSource()));
        return type.cast(factory.getProxy());
    }
    private ChapterUpdateDTO change(Long version) { return ChapterUpdateDTO.builder().title("Edited title").summary("Edited summary").bodyMarkdown("Edited body").rowVersion(version).build(); }
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
    @Test void workspaceSaveIsAtomicPublicRevisionStaysFrozenAndStaleClientGetsHttp409() throws Exception {
        var originalChapter=content.chapter(chapter.getId());
        var saved=content.updateChapter(chapter.getId(),change(originalChapter.getRowVersion()));
        assertEquals("Edited title",saved.getTitle());assertEquals("Edited summary",saved.getSummary());assertEquals("Edited body",saved.getBodyMarkdown());
        assertEquals(originalChapter.getRowVersion()+1,saved.getRowVersion());
        assertEquals("Original body",publication.publicChapter("java","intro").path("bodyMarkdown").asText());
        var mvc=MockMvcBuilders.standaloneSetup(new TutorialStructureAdminController(content,publication)).setControllerAdvice(new GlobalApiExceptionHandler()).build();
        mvc.perform(patch("/api/admin/tutorial-chapters/"+chapter.getId()).contentType("application/json").content(json.writeValueAsString(change(originalChapter.getRowVersion()))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TUTORIAL_CONFLICT"));
        publication.publishChapter(chapter.getId());
        assertEquals("Edited body",publication.publicChapter("java","intro").path("bodyMarkdown").asText());
    }
    @Test void mediaReferenceFailureRollsBackTitleSummaryBodyAndVersionInRealSpringTransaction() {
        var before=content.chapter(chapter.getId());
        doThrow(new IllegalStateException("media failure")).when(media).replaceChapter(eq(chapter.getId()),anyString());
        assertThrows(IllegalStateException.class,()->content.updateChapter(chapter.getId(),change(before.getRowVersion())));
        var after=content.chapter(chapter.getId());
        assertEquals(before.getTitle(),after.getTitle());assertEquals(before.getSummary(),after.getSummary());
        assertEquals(before.getBodyMarkdown(),after.getBodyMarkdown());assertEquals(before.getRowVersion(),after.getRowVersion());
    }
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
