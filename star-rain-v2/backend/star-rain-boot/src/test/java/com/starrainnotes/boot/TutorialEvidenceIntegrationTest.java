package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.tutorial.content.entity.*;
import com.starrainnotes.tutorial.content.mapper.*;
import com.starrainnotes.tutorial.content.service.TutorialContentService;
import com.starrainnotes.tutorial.content.service.impl.TutorialPublicationServiceImpl;
import com.starrainnotes.tutorial.content.event.TutorialEventPublisher;
import com.starrainnotes.tutorial.content.media.TutorialMediaReferences;
import com.starrainnotes.tutorial.learning.dto.*;
import com.starrainnotes.tutorial.learning.entity.EvidenceModels.*;
import com.starrainnotes.tutorial.learning.exception.*;
import com.starrainnotes.tutorial.learning.mapper.*;
import com.starrainnotes.tutorial.learning.service.impl.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.*;

/** Real content publication + plan/session/evidence/answer SQL. All probes roll back. */
class TutorialEvidenceIntegrationTest extends MapperXmlIntegrationSupport {
    private SqlSession sql;
    private EvidenceLearningMapper mapper;
    private TutorialMapper tutorials;
    private TutorialChapterMapper chapters;
    private TutorialKnowledgeCardMapper cards;
    private TutorialQuestionMapper questions;
    private TutorialPublicationServiceImpl publications;
    private EvidenceStudyPlanService plans;
    private EvidenceLearningService learning;
    private CurrentActorApi actors;
    private Long accountId;
    private final AtomicLong probe=new AtomicLong(System.nanoTime()%1_000_000L);

    @BeforeEach void setup() throws java.sql.SQLException {
        sql=openSession();
        // Match the application's Hikari connection-init-sql; DATETIME facts are UTC.
        try (var statement=sql.getConnection().createStatement()) { statement.execute("SET time_zone = '+00:00'"); }
        mapper=sql.getMapper(EvidenceLearningMapper.class);
        tutorials=sql.getMapper(TutorialMapper.class);chapters=sql.getMapper(TutorialChapterMapper.class);
        cards=sql.getMapper(TutorialKnowledgeCardMapper.class);questions=sql.getMapper(TutorialQuestionMapper.class);
        actors=mock(CurrentActorApi.class);accountId=900_900_000_000L+probe.incrementAndGet();
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(accountId).build());
        ObjectMapper json=new ObjectMapper().findAndRegisterModules();
        LearningContentAccess content=new LearningContentAccess(tutorials,sql.getMapper(TutorialRevisionMapper.class),chapters,cards,questions,json);
        plans=new EvidenceStudyPlanService(actors,content,mapper);
        learning=new EvidenceLearningService(actors,content,tutorials,mapper,sql.getMapper(LearningProgressMapper.class),plans,
                new LearningEventWriter(sql.getMapper(LearningHistoryMapper.class)),json);
        publications=new TutorialPublicationServiceImpl(tutorials,sql.getMapper(TutorialCategoryMapper.class),sql.getMapper(TutorialGroupMapper.class),chapters,cards,questions,
                sql.getMapper(TutorialQuestionCardMapper.class),sql.getMapper(TutorialRevisionMapper.class),mock(TutorialContentService.class),actors,json,
                mock(TutorialEventPublisher.class),mock(TutorialMediaReferences.class));
    }
    @AfterEach void rollback() { if(sql!=null) { sql.rollback();sql.close(); } }

    private Fixture fixture(boolean withQuestion,int... cardCounts) {
        TutorialCategoryEntity category=new TutorialCategoryEntity();category.setName("Evidence probe");category.setSlug("evidence-cat-"+System.nanoTime());category.setSortOrder(0);
        sql.getMapper(TutorialCategoryMapper.class).insert(category);
        TutorialEntity tutorial=new TutorialEntity();tutorial.setCategoryId(category.getId());tutorial.setSlug("evidence-tutorial-"+System.nanoTime());
        tutorial.setTitle("Evidence probe tutorial");tutorial.setSummary("probe");tutorial.setSortOrder(0);tutorial.setPublicationStatus("NEVER_PUBLISHED");tutorial.setEditingStatus("DRAFT");
        tutorial.setCreatedByAccountId(accountId);tutorial.setUpdatedByAccountId(accountId);tutorials.insert(tutorial);
        TutorialGroupEntity group=new TutorialGroupEntity();group.setTutorialId(tutorial.getId());group.setTitle("Group");group.setSortOrder(0);group.setStatus("ACTIVE");
        sql.getMapper(TutorialGroupMapper.class).insert(group);
        Fixture fixture=new Fixture();fixture.tutorial=tutorial;fixture.groupId=group.getId();
        int order=0;
        for(int count:cardCounts) {
            TutorialChapterEntity chapter=new TutorialChapterEntity();chapter.setTutorialId(tutorial.getId());chapter.setGroupId(group.getId());chapter.setTitle("Chapter "+order);
            chapter.setSlug("chapter-"+order);chapter.setBodyMarkdown("# Body\n\nUnderstand the content.");chapter.setSortOrder(order++);chapter.setStatus("PUBLISHED");chapters.insert(chapter);fixture.chapters.add(chapter);
            for(int i=0;i<count;i++) {
                TutorialKnowledgeCardEntity card=new TutorialKnowledgeCardEntity();card.setChapterId(chapter.getId());card.setFrontText("Recall "+i);card.setBackMarkdown("Answer "+i);card.setSortOrder(i);card.setStatus("ENABLED");cards.insert(card);fixture.cards.add(card);
            }
            if(withQuestion) {
                TutorialQuestionEntity question=new TutorialQuestionEntity();question.setChapterId(chapter.getId());question.setQuestionText("Explain");question.setReferenceAnswer("Reference");question.setSortOrder(0);question.setStatus("ENABLED");questions.insert(question);fixture.questions.add(question);
                if(count>0) sql.getMapper(TutorialQuestionCardMapper.class).insert(question.getId(),fixture.cards.get(fixture.cards.size()-1).getId(),0);
            }
        }
        publications.publish(tutorial.getId());return fixture;
    }
    private EvidencePlanDTO request(Fixture fixture) {
        EvidencePlanDTO dto=new EvidencePlanDTO();dto.setName("Learning probe");dto.setTutorialId(fixture.tutorial.getId());dto.setEntireTutorial(true);return dto;
    }
    private Session rateAll(Session session,String rating) {
        for(Item item:session.getItems()) if(!"COMPLETED".equals(item.getStatus())) {
            session=learning.reveal(session.getId(),item.getKnowledgeCardId(),false);
            EvidenceRatingDTO request=new EvidenceRatingDTO();request.setRating(rating);session=learning.rate(session.getId(),item.getKnowledgeCardId(),request,false);
        }
        return session;
    }
    private void answer(Long questionId,String phase,String text) { AnswerVersionDTO dto=new AnswerVersionDTO();dto.setAnswerText(text);dto.setAnswerPhase(phase);learning.saveVersion(questionId,dto); }

    @Test void filteredHistoryAndMasteryPaginateAfterFilteringAndKeepAccountsIsolated() {
        Fixture fixture=fixture(false,1,1);Plan plan=plans.create(request(fixture));plan=plans.transition(plan.getId(),"activate");
        assertEquals(fixture.tutorial.getSlug(),plan.getTutorialSlug());
        assertEquals("chapter-0",plan.getChapters().get(0).getChapterSlug());
        Long taskId=plan.getTasks().get(0).getId();
        Session first=learning.initial(fixture.chapters.get(0).getId(),taskId);rateAll(first,"REMEMBERED");
        Session second=learning.initial(fixture.chapters.get(1).getId(),taskId);
        LearningPageQueryDTO query=new LearningPageQueryDTO();query.setTutorialId(fixture.tutorial.getId());query.setPageSize(1);
        var history=learning.history(query);assertEquals(2,history.getTotal());assertEquals(second.getId(),history.getItems().get(0).getId());
        query.setPage(2);assertEquals(first.getId(),learning.history(query).getItems().get(0).getId());
        query.setPage(1);query.setStatus("COMPLETED");assertEquals(1,learning.history(query).getTotal());
        query.setSessionType("REVIEW");assertEquals(0,learning.history(query).getTotal());query.setSessionType("INITIAL_STUDY");
        query.setKeyword("Learning probe");assertEquals(1,learning.history(query).getTotal());
        query.setKeyword("%'");assertEquals(0,learning.history(query).getTotal());query.setKeyword(null);
        var today=java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).toLocalDate();
        query.setFromDate(today);query.setToDate(today);assertEquals(1,learning.history(query).getTotal());
        query.setFromDate(today.plusDays(1));query.setToDate(today.plusDays(1));assertEquals(0,learning.history(query).getTotal());
        query.setFromDate(today.plusDays(2));assertThrows(LearningInvalidRequestException.class,()->learning.history(query));
        LearningPageQueryDTO masteryQuery=new LearningPageQueryDTO();masteryQuery.setTutorialId(fixture.tutorial.getId());masteryQuery.setPageSize(1);masteryQuery.setStatus("UNLEARNED");
        var unlearned=learning.masteryPage(masteryQuery);assertEquals(1,unlearned.getTotal());assertEquals(fixture.cards.get(1).getId(),unlearned.getItems().get(0).getKnowledgeCardId());
        masteryQuery.setStatus("LEARNING");assertEquals(fixture.cards.get(0).getId(),learning.masteryPage(masteryQuery).getItems().get(0).getKnowledgeCardId());
        masteryQuery.setNeedsRevalidation(true);assertEquals(0,learning.masteryPage(masteryQuery).getTotal());
        assertTrue(learning.historyOptions().stream().anyMatch(o->o.getId().equals(fixture.tutorial.getId())));
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(accountId+100L).build());
        assertEquals(0,learning.history(new LearningPageQueryDTO()).getTotal());assertTrue(learning.historyOptions().isEmpty());
        Long foreignPlanId=plan.getId();assertThrows(LearningResourceNotFoundException.class,()->plans.get(foreignPlanId));
        masteryQuery.setNeedsRevalidation(null);masteryQuery.setStatus("UNLEARNED");assertEquals(2,learning.masteryPage(masteryQuery).getTotal());
    }

    @Test void completedPlansReleaseActiveListQuotaAndRemainInPagedHistory() {
        Fixture fixture=fixture(false,1);Plan plan=plans.create(request(fixture));plan=plans.transition(plan.getId(),"activate");
        rateAll(learning.initial(fixture.chapters.get(0).getId(),plan.getTasks().get(0).getId()),"REMEMBERED");
        assertEquals("COMPLETED",plans.get(plan.getId()).getStatus());assertTrue(plans.list(true).isEmpty());
        LearningPageQueryDTO query=new LearningPageQueryDTO();query.setKeyword("Learning probe");
        assertEquals(1,plans.history(query).getTotal());assertEquals(plan.getId(),plans.history(query).getItems().get(0).getId());
        query.setStatus("CANCELLED");assertEquals(0,plans.history(query).getTotal());
        assertTrue(plans.list().stream().noneMatch(p->List.of("ACTIVE","DRAFT","PAUSED").contains(p.getStatus())));
    }

    @Test void repeatedPlanNeedsNewEvidenceAndAnswersAndKeepsPreviousRounds() {
        Fixture fixture=fixture(true,1);
        Plan plan=plans.transition(plans.create(request(fixture)).getId(),"activate");
        Long chapterId=fixture.chapters.get(0).getId(), questionId=fixture.questions.get(0).getId();
        Session first=rateAll(learning.initial(chapterId,plan.getTasks().get(0).getId()),"REMEMBERED");
        answer(questionId,"BEFORE_REFERENCE","First independent answer");
        assertEquals("COMPLETED",plans.get(plan.getId()).getStatus());
        Plan restarted=plans.transition(plan.getId(),"restart");
        assertEquals(plan.getId(),restarted.getId());assertEquals(2,restarted.getStudyRound());
        assertEquals(0,restarted.getCompletedChapters());assertEquals("DRAFT",restarted.getStatus());
        assertThrows(LearningStateConflictException.class,()->plans.start(plan.getTasks().get(0).getId()));
        restarted=plans.transition(plan.getId(),"activate");
        assertEquals("ACTIVE",restarted.getStatus());assertFalse(learning.studyState(chapterId,plan.getId()).getCompleted());
        Session second=learning.initial(chapterId,restarted.getTasks().get(0).getId());
        assertNotEquals(first.getId(),second.getId());assertEquals(2,second.getStudyRound());assertEquals(1,second.getItems().size());
        rateAll(second,"REMEMBERED");
        assertEquals("ACTIVE",plans.get(plan.getId()).getStatus());
        assertFalse(learning.studyState(chapterId,plan.getId()).getQuestions().get(0).getAnswered());
        answer(questionId,"AFTER_REFERENCE","Only improving the old answer is insufficient");
        assertEquals("ACTIVE",plans.get(plan.getId()).getStatus());
        answer(questionId,"BEFORE_REFERENCE","Second independent answer");
        assertEquals("COMPLETED",plans.get(plan.getId()).getStatus());
        assertEquals(2,mapper.cardEvidence(accountId,fixture.cards.get(0).getId()).size());
        assertEquals(3,mapper.answerVersions(accountId,questionId).size());
        assertEquals(1,learning.session(first.getId()).getSummary().getQuestionCount());
        assertEquals(2,mapper.countHistorySessions(accountId));
        EvidencePlanDTO sameScope=request(fixture);sameScope.setName("A different display name");
        Plan reused=plans.create(sameScope);assertEquals(plan.getId(),reused.getId());assertEquals(3,reused.getStudyRound());
        assertEquals(1,mapper.listPlans(accountId).size());
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(accountId+1).build());
        assertThrows(LearningResourceNotFoundException.class,()->plans.restart(plan.getId()));
    }

    @Test void identicalNamesWithDifferentKnowledgeScopesAreIndependentPlans() {
        Fixture fixture=fixture(false,1,1);
        EvidencePlanDTO one=request(fixture);one.setEntireTutorial(false);one.setChapterIds(List.of(fixture.chapters.get(0).getId()));
        EvidencePlanDTO two=request(fixture);two.setEntireTutorial(false);two.setChapterIds(List.of(fixture.chapters.get(1).getId()));
        Plan first=plans.create(one),second=plans.create(two);
        assertEquals(first.getName(),second.getName());assertNotEquals(first.getId(),second.getId());
        assertEquals(1,first.getTotalChapters());assertEquals(1,second.getTotalChapters());
    }

    @Test void deletingAuthoringContentRetiresPlansAndPreservesLearningFacts() {
        Fixture fixture=fixture(true,1,1);Plan plan=plans.create(request(fixture));plans.transition(plan.getId(),"activate");
        Long chapterId=fixture.chapters.get(0).getId(), cardId=fixture.cards.get(0).getId(), questionId=fixture.questions.get(0).getId();
        Session initial=learning.initial(chapterId,plan.getTasks().get(0).getId());rateAll(initial,"REMEMBERED");
        answer(questionId,"BEFORE_REFERENCE","Independent answer worth preserving");
        Fixture unrelated=fixture(false,1);Plan other=plans.create(request(unrelated));plans.transition(other.getId(),"activate");
        var cleanup=new TutorialDependencyCleanup(new org.springframework.jdbc.core.JdbcTemplate(
                new org.springframework.jdbc.datasource.SingleConnectionDataSource(sql.getConnection(),true)),
                sql.getMapper(TutorialLearningCleanupMapper.class));
        cleanup.chapterChildren(chapterId);sql.clearCache();
        assertEquals("CANCELLED",plans.get(plan.getId()).getStatus());
        assertEquals("SKIPPED",plans.task(plan.getTasks().get(0).getId()).getStatus());
        assertEquals("ACTIVE",plans.get(other.getId()).getStatus());
        assertEquals(1,mapper.cardEvidence(accountId,cardId).size());
        assertEquals(1,mapper.masteries(accountId).stream().filter(m->m.getKnowledgeCardId().equals(cardId)).count());
        assertEquals("Independent answer worth preserving",mapper.answerVersions(accountId,questionId).get(0).getAnswerText());
        assertEquals(1,mapper.countHistorySessions(accountId));
        assertEquals(1,learning.session(initial.getId()).getSummary().getQuestionCount());
        assertTrue(sql.getMapper(TutorialQuestionCardMapper.class).cardIds(questionId).isEmpty());
        assertNull(cards.selectById(cardId));assertNull(questions.selectById(questionId));
        cleanup.groupLearning(unrelated.groupId);sql.clearCache();assertEquals("CANCELLED",plans.get(other.getId()).getStatus());
        Fixture third=fixture(false,1);Plan thirdPlan=plans.create(request(third));
        cleanup.tutorialLearning(third.tutorial.getId());sql.clearCache();assertEquals("CANCELLED",plans.get(thirdPlan.getId()).getStatus());
    }

    @Test void completeAuthorToInitialStudyAnswerAndReviewLoop() throws Exception {
        Fixture fixture=fixture(true,7,5);Plan preview=plans.preview(request(fixture),null);Plan plan=plans.create(request(fixture));
        assertEquals(preview.getTasks().stream().map(Task::getCardCount).toList(),plan.getTasks().stream().map(Task::getCardCount).toList());
        assertEquals(List.of(12),plan.getTasks().stream().map(Task::getCardCount).toList());plans.transition(plan.getId(),"activate");
        Long taskId=plan.getTasks().get(0).getId();
        for(TutorialChapterEntity chapter:fixture.chapters) {
            Session initial=learning.initial(chapter.getId(),taskId);assertNull(initial.getItems().get(0).getBackMarkdown());
            assertEquals(initial.getId(),learning.initial(chapter.getId(),taskId).getId(),"refresh resumes frozen session");
            Long firstCard=initial.getItems().get(0).getKnowledgeCardId();EvidenceRatingDTO rating=new EvidenceRatingDTO();rating.setRating("REMEMBERED");
            final Long initialId=initial.getId();assertThrows(LearningStateConflictException.class,()->learning.rate(initialId,firstCard,rating,false));
            initial=rateAll(initial,"REMEMBERED");assertEquals("IN_PROGRESS",initial.getStatus());
            assertFalse(learning.studyState(chapter.getId()).getCompleted());
            Long questionId=fixture.questions.stream().filter(q->q.getChapterId().equals(chapter.getId())).findFirst().orElseThrow().getId();
            assertThrows(LearningAnswerLockedException.class,()->learning.reference(questionId));
            answer(questionId,"BEFORE_REFERENCE","My independent answer");assertEquals("Reference",learning.reference(questionId));
            answer(questionId,"AFTER_REFERENCE","Improved answer");answer(questionId,"BEFORE_REFERENCE","Later independent answer");
            assertEquals(List.of(1,2,3),learning.versions(questionId).stream().map(AnswerVersion::getVersionNo).toList());
            assertEquals("My independent answer",learning.versions(questionId).get(0).getAnswerText());
            assertTrue(learning.studyState(chapter.getId()).getCompleted());assertNull(learning.initial(chapter.getId(),taskId));
            learning.rate(initialId,firstCard,rating,false);assertEquals(1,mapper.cardEvidence(accountId,firstCard).size());
        }
        assertEquals("COMPLETED",plans.task(taskId).getStatus());assertEquals("COMPLETED",plans.get(plan.getId()).getStatus());
        assertEquals(12,learning.statistics().getLearnedCardCount());
        ReviewSessionDTO review=new ReviewSessionDTO();review.setCount(12);
        for(int i=0;i<2;i++) { Session session=learning.review(review);assertThrows(LearningStateConflictException.class,()->learning.review(review));session=rateAll(session,"REMEMBERED");assertEquals("COMPLETED",session.getStatus());assertEquals(12,session.getSummary().getRememberedCount()); }
        assertEquals(12,learning.statistics().getStableMasteredCount());review.setMode("STABLE_AUDIT");
        Session audit=rateAll(learning.review(review),"FORGOT");assertEquals(12,audit.getSummary().getDowngradedCount());
        assertEquals(12,learning.statistics().getBasicMasteredCount());
        review.setMode("RECOMMENDED");rateAll(learning.review(review),"FORGOT");assertEquals(12,learning.statistics().getLearningCount());
        assertEquals(6,learning.history(1,20).getTotal());
        try(var query=sql.getConnection().prepareStatement("SELECT COUNT(*) FROM sr_review_schedule WHERE account_id=?")) {
            query.setLong(1,accountId);try(var result=query.executeQuery()) { result.next();assertEquals(0,result.getLong(1),"new learning never writes legacy schedules"); }
        }
    }

    @Test void fivePlansCanBeActiveSixthIsRejectedAndCancellationReleasesQuota() {
        Fixture fixture=fixture(false,1,1,1,1,1,1);List<Plan> active=new ArrayList<>();
        for(int i=0;i<5;i++) { EvidencePlanDTO request=request(fixture);request.setEntireTutorial(false);request.setChapterIds(List.of(fixture.chapters.get(i).getId()));Plan plan=plans.create(request);active.add(plans.transition(plan.getId(),"activate")); }
        assertEquals(5,plans.list().stream().filter(p->"ACTIVE".equals(p.getStatus())).count());
        EvidencePlanDTO sixth=request(fixture);sixth.setEntireTutorial(false);sixth.setChapterIds(List.of(fixture.chapters.get(5).getId()));
        assertThrows(LearningStateConflictException.class,()->plans.create(sixth));plans.transition(active.get(0).getId(),"cancel");assertNotNull(plans.create(sixth).getId());
        assertThrows(LearningStateConflictException.class,()->plans.update(active.get(1).getId(),sixth));
    }

    @Test void contentRevisionRevalidatesWithoutDestroyingEvidenceOrMastery() {
        Fixture fixture=fixture(false,1);Long chapterId=fixture.chapters.get(0).getId();rateAll(learning.initial(chapterId,null),"REMEMBERED");
        ReviewSessionDTO request=new ReviewSessionDTO();request.setCount(1);rateAll(learning.review(request),"REMEMBERED");rateAll(learning.review(request),"REMEMBERED");
        TutorialKnowledgeCardEntity card=fixture.cards.get(0);publications.withdraw(fixture.tutorial.getId());
        cards.updateContent(card.getId(),card.getFrontText(),"Updated explanation","ENABLED",LocalDateTime.now(ZoneOffset.UTC));publications.publish(fixture.tutorial.getId());
        CardMastery updated=learning.mastery(fixture.tutorial.getId(),chapterId).get(0);
        assertEquals("STABLE_MASTERED",updated.getMasteryStatus());assertTrue(updated.getNeedsRevalidation());assertEquals(0,updated.getPriority());assertEquals(3,mapper.cardEvidence(accountId,card.getId()).size());
        Session review=rateAll(learning.review(request),"REMEMBERED");assertEquals(1,review.getSummary().getRevalidationCount());
        assertFalse(learning.mastery(fixture.tutorial.getId(),chapterId).get(0).getNeedsRevalidation());
        assertEquals("REVALIDATION",mapper.cardEvidence(accountId,card.getId()).get(0).getEvidenceType());
    }

    @Test void disabledPublishedCardLeavesRecommendationButKeepsHistoryAndOwnerIsolation() {
        Fixture fixture=fixture(false,1);Long chapterId=fixture.chapters.get(0).getId();Session initial=rateAll(learning.initial(chapterId,null),"REMEMBERED");
        assertEquals(1,learning.reviewSummary().getCandidateCount());
        publications.withdraw(fixture.tutorial.getId());TutorialKnowledgeCardEntity card=fixture.cards.get(0);
        cards.updateContent(card.getId(),card.getFrontText(),card.getBackMarkdown(),"DISABLED",LocalDateTime.now(ZoneOffset.UTC));publications.publish(fixture.tutorial.getId());
        assertEquals(0,learning.reviewSummary().getCandidateCount());assertEquals(1,learning.history(1,20).getTotal());
        assertEquals(1,mapper.cardEvidence(accountId,card.getId()).size());
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(accountId+1).build());
        assertThrows(LearningResourceNotFoundException.class,()->learning.session(initial.getId()));assertEquals(0,learning.history(1,20).getTotal());
    }

    @Test void draftEditRebuildsAndConflictScopeRejectsDuplicates() {
        Fixture fixture=fixture(false,3,3,36);EvidencePlanDTO request=request(fixture);request.setEntireTutorial(false);request.setChapterIds(List.of(fixture.chapters.get(0).getId(),fixture.chapters.get(2).getId()));
        Plan plan=plans.create(request);assertEquals(List.of(3,36),plan.getTasks().stream().map(Task::getCardCount).toList());
        assertThrows(LearningStateConflictException.class,()->plans.create(request));request.setChapterIds(List.of(fixture.chapters.get(0).getId(),fixture.chapters.get(1).getId()));
        Plan edited=plans.update(plan.getId(),request);assertEquals(List.of(6),edited.getTasks().stream().map(Task::getCardCount).toList());
        assertEquals(2,edited.getChapters().size());plans.transition(plan.getId(),"activate");plans.transition(plan.getId(),"pause");plans.transition(plan.getId(),"resume");
        assertEquals("ACTIVE",plans.get(plan.getId()).getStatus());
    }

    @Test void databaseEnforcesOneInitialPerAccountCardAndOneFactPerSession() {
        Fixture fixture=fixture(false,1);Session session=rateAll(learning.initial(fixture.chapters.get(0).getId(),null),"FUZZY");
        Evidence duplicate=mapper.cardEvidence(accountId,fixture.cards.get(0).getId()).get(0);duplicate.setSessionId(session.getId()+9999);
        assertThrows(org.apache.ibatis.exceptions.PersistenceException.class,()->mapper.insertEvidence(duplicate));
    }

    private static class Fixture {
        TutorialEntity tutorial;Long groupId;
        List<TutorialChapterEntity> chapters=new ArrayList<>();List<TutorialKnowledgeCardEntity> cards=new ArrayList<>();List<TutorialQuestionEntity> questions=new ArrayList<>();
    }
}
