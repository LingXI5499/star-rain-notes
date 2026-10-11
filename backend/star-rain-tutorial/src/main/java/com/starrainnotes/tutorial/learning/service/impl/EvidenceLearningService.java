package com.starrainnotes.tutorial.learning.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.learning.dto.*;
import com.starrainnotes.tutorial.learning.entity.EvidenceModels.*;
import com.starrainnotes.tutorial.learning.exception.*;
import com.starrainnotes.tutorial.learning.mapper.EvidenceLearningMapper;
import com.starrainnotes.tutorial.learning.mapper.LearningProgressMapper;
import com.starrainnotes.tutorial.learning.service.EvidenceAlgorithms;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EvidenceLearningService {
    private final CurrentActorApi actors;
    private final LearningContentAccess content;
    private final TutorialMapper tutorials;
    private final EvidenceLearningMapper mapper;
    private final LearningProgressMapper progress;
    private final EvidenceStudyPlanService plans;
    private final LearningEventWriter events;
    private final ObjectMapper json;

    private Long actor() { return actors.current().getAccountId(); }
    private void lock(Long accountId) { mapper.ensureAccountLock(accountId);mapper.lockAccount(accountId); }
    private LearningStateConflictException conflict(String message) { return new LearningStateConflictException(message); }

    public StudyState studyState(Long chapterId) { return studyState(chapterId,null); }

    public StudyState studyState(Long chapterId, Long planId) {
        Long accountId = actor();LearningContentAccess.ChapterRef chapter=content.chapter(chapterId);
        StudyState state = new StudyState();state.setChapterId(chapterId);state.setTitle(chapter.getTitle());
        state.setTutorialSlug(chapter.getTutorialSlug());state.setChapterSlug(chapter.getChapterSlug());
        state.setBodyMarkdown(chapter.getSnapshot().path("bodyMarkdown").asText());
        Plan plan = planId == null ? null : plans.get(planId);
        if (plan != null && plan.getChapters().stream().noneMatch(c -> c.getChapterId().equals(chapterId))) throw new LearningResourceNotFoundException();
        Session current = planId == null ? mapper.latestInitial(accountId,chapterId) : mapper.planSession(accountId,chapterId,planId);
        state.setInitialCardIds(plan == null ? mapper.initialCardIds(accountId,chapterId) : current == null ? List.of() :
                mapper.items(accountId,current.getId()).stream().filter(i -> "COMPLETED".equals(i.getStatus())).map(Item::getKnowledgeCardId).toList());
        List<String> requiredCards = new ArrayList<>();
        chapter.getSnapshot().path("cards").forEach(card -> requiredCards.add(card.path("id").asText()));
        state.setRequiredCardIds(requiredCards);
        Set<Long> answered=new HashSet<>(plan == null ? mapper.answeredQuestionIds(accountId) : current == null ? List.of() : mapper.answeredForSession(accountId,current.getId()));
        List<Question> questions=new ArrayList<>();
        for (JsonNode node:chapter.getSnapshot().path("questions")) {
            Question question=new Question();question.setId(node.path("id").asLong());
            question.setQuestionText(node.path("questionText").asText());question.setAnswered(answered.contains(question.getId()));
            List<Long> cardIds=new ArrayList<>();node.path("knowledgeCardIds").forEach(id->cardIds.add(id.asLong()));
            question.setKnowledgeCardIds(cardIds);questions.add(question);
        }
        state.setQuestions(questions);state.setCompleted(plan == null ? eligible(accountId,chapter) : plan.getChapters().stream().anyMatch(c -> c.getChapterId().equals(chapterId) && Boolean.TRUE.equals(c.getCompleted())));
        state.setSession(current==null?null:sessionView(accountId,current));return state;
    }

    @Transactional
    public Session initial(Long chapterId, Long taskId) {
        Long accountId=actor();lock(accountId);LearningContentAccess.ChapterRef chapter=content.chapter(chapterId);
        Task task=null;
        if (taskId!=null) {
            task=plans.task(taskId);
            if (task.getChapters().stream().noneMatch(c->c.getChapterId().equals(chapterId))) throw conflict("章节不属于此任务");
            Plan plan=plans.get(task.getPlanId());
            if (!task.getStudyRound().equals(plan.getStudyRound())) throw conflict("此任务属于历史学习轮次");
            if (plan.getChapters().stream().anyMatch(c->c.getChapterId().equals(chapterId) && Boolean.TRUE.equals(c.getCompleted()))) {
                reconcile(accountId,chapter);return null;
            }
        }
        if (taskId == null && eligible(accountId,chapter)) {
            reconcile(accountId,chapter);return null;
        }
        if (taskId!=null) task=plans.start(taskId);
        Session current=task == null ? mapper.currentInitial(accountId,chapterId) : mapper.planSession(accountId,chapterId,task.getPlanId());
        if (current!=null) return sessionView(accountId,current);
        if (chapter.getSnapshot().path("cards").isEmpty()) throw conflict("本章尚未设置知识卡片，暂不能开始首次学习");
        Session session=new Session();session.setAccountId(accountId);session.setChapterId(chapterId);
        session.setTutorialId(chapter.getTutorialId());session.setSessionType("INITIAL_STUDY");session.setStatus("IN_PROGRESS");
        if (task!=null) { session.setStudyTaskId(taskId);session.setStudyPlanId(task.getPlanId());session.setStudyRound(task.getStudyRound()); }
        mapper.insertSession(session);
        if (task != null) for (JsonNode question : chapter.getSnapshot().path("questions")) {
            Long questionId = question.path("id").asLong();
            Answer previous = mapper.answer(accountId,questionId);
            mapper.insertSessionQuestion(session.getId(),questionId,previous == null ? 0 : previous.getVersionCount());
        }
        Set<Long> done=task == null ? new HashSet<>(mapper.initialCardIds(accountId,chapterId)) : Set.of();
        int sequence=0;
        for (JsonNode card:chapter.getSnapshot().path("cards")) {
            if (!done.contains(card.path("id").asLong())) {
                Item item=new Item();item.setAccountId(accountId);item.setSessionId(session.getId());
                item.setTutorialId(chapter.getTutorialId());item.setChapterId(chapterId);
                item.setKnowledgeCardId(card.path("id").asLong());item.setCardContentVersion(card.path("contentVersion").asInt(1));
                item.setFrontText(card.path("frontText").asText());item.setBackMarkdown(card.path("backMarkdown").asText());
                item.setSequenceNo(++sequence);mapper.insertItem(item);
            }
        }
        progress.ensureRow(accountId,chapter.getTutorialId(),chapter.getGroupId(),chapterId);
        return sessionView(accountId,session);
    }

    private Session requireSession(Long accountId,Long id) {
        Session session=mapper.session(accountId,id);
        if (session==null) throw new LearningResourceNotFoundException();return session;
    }

    public Session session(Long id) { return sessionView(actor(),requireSession(actor(),id)); }
    public Session reviewSession(Long id) {
        Session session=requireSession(actor(),id);
        if ("INITIAL_STUDY".equals(session.getSessionType())) throw new LearningResourceNotFoundException();
        return sessionView(actor(),session);
    }

    private Session sessionView(Long accountId,Session session) {
        List<Item> items=mapper.items(accountId,session.getId());
        for(Item item:items) if(item.getRevealedAt()==null) item.setBackMarkdown(null);
        session.setItems(items);session.setSummary(summary(accountId,session));return session;
    }

    private Item activeItem(Long accountId,Session session,Long cardId) {
        if (session.getStudyPlanId() != null) {
            Plan plan = plans.get(session.getStudyPlanId());
            if (!"ACTIVE".equals(plan.getStatus()) || !session.getStudyRound().equals(plan.getStudyRound())) throw conflict("请在当前轮次的进行中计划内学习");
        }
        if(!"IN_PROGRESS".equals(session.getStatus())) throw conflict("此学习会话已结束");
        List<Item> items=mapper.items(accountId,session.getId());
        Item item=items.stream().filter(i->i.getKnowledgeCardId().equals(cardId)).findFirst().orElseThrow(LearningResourceNotFoundException::new);
        if(!"COMPLETED".equals(item.getStatus())) {
            Item next=items.stream().filter(i->!"COMPLETED".equals(i.getStatus())).findFirst().orElseThrow();
            if(!next.getKnowledgeCardId().equals(cardId)) throw conflict("请按会话顺序完成知识卡片");
            content.card(cardId); // Withdrawn or removed published content cannot produce new evidence.
        }
        return item;
    }

    @Transactional
    public Session reveal(Long id,Long cardId,boolean reviewOnly) {
        Long accountId=actor();lock(accountId);Session session=requireSession(accountId,id);
        checkRoute(session,reviewOnly);activeItem(accountId,session,cardId);
        mapper.revealItem(accountId,id,cardId);return sessionView(accountId,session);
    }

    private void checkRoute(Session session,boolean reviewOnly) {
        if(reviewOnly && "INITIAL_STUDY".equals(session.getSessionType())) throw new LearningResourceNotFoundException();
    }

    @Transactional
    public Session rate(Long id,Long cardId,EvidenceRatingDTO request,boolean reviewOnly) {
        Long accountId=actor();lock(accountId);Session session=requireSession(accountId,id);checkRoute(session,reviewOnly);
        // Completed retries return the frozen result, without a second fact or history event.
        Item existing=mapper.items(accountId,id).stream().filter(i->i.getKnowledgeCardId().equals(cardId))
                .findFirst().orElseThrow(LearningResourceNotFoundException::new);
        if("COMPLETED".equals(existing.getStatus())) return sessionView(accountId,session);
        Item item=activeItem(accountId,session,cardId);
        if(item.getRevealedAt()==null) throw conflict("请先查看答案，再评价本次回忆");
        try { EvidenceAlgorithms.score(request.getRating()); } catch(IllegalArgumentException exception) { throw new LearningInvalidRequestException("请选择忘记、模糊或记得"); }
        List<Evidence> history=mapper.cardEvidence(accountId,cardId);
        String previous=aggregate(history).getMasteryStatus();

        Evidence evidence=new Evidence();evidence.setAccountId(accountId);evidence.setSessionId(id);
        evidence.setTutorialId(item.getTutorialId());evidence.setChapterId(item.getChapterId());evidence.setKnowledgeCardId(cardId);
        evidence.setCardContentVersion(item.getCardContentVersion());evidence.setRating(request.getRating());
        String type="INITIAL_STUDY".equals(session.getSessionType())?(history.stream().anyMatch(e -> "INITIAL".equals(e.getEvidenceType())) ? "REVIEW" : "INITIAL"):
                Objects.equals(item.getSourcePriority(),0)?"REVALIDATION":
                "STABLE_AUDIT".equals(session.getSessionType())?"STABLE_AUDIT":"REVIEW";
        evidence.setEvidenceType(type);evidence.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        evidence.setPreviousMasteryStatus(previous);history.add(0,evidence);
        Mastery mastery=aggregate(history);mastery.setAccountId(accountId);mastery.setKnowledgeCardId(cardId);
        evidence.setMasteryStatus(mastery.getMasteryStatus());mapper.insertEvidence(evidence);mapper.upsertMastery(mastery);
        mapper.completeItem(accountId,id,cardId);
        if("INITIAL_STUDY".equals(session.getSessionType())) reconcile(accountId,content.chapter(item.getChapterId()));
        else if(mapper.items(accountId,id).stream().allMatch(i->"COMPLETED".equals(i.getStatus()))) finish(accountId,session);
        return sessionView(accountId,requireSession(accountId,id));
    }

    private Mastery aggregate(List<Evidence> newestFirst) {
        Mastery result=new Mastery();int forgot=0,fuzzy=0,remembered=0,recent=0;
        for(int i=0;i<newestFirst.size();i++) {
            String rating=newestFirst.get(i).getRating();
            if("FORGOT".equals(rating)) forgot++;else if("FUZZY".equals(rating)) fuzzy++;else remembered++;
            if(i<3) recent+=EvidenceAlgorithms.score(rating);
        }
        result.setEvidenceCount(newestFirst.size());result.setForgotCount(forgot);result.setFuzzyCount(fuzzy);
        result.setRememberedCount(remembered);result.setRecentScore(recent);
        result.setMasteryStatus(EvidenceAlgorithms.mastery(newestFirst.size(),recent));
        if(!newestFirst.isEmpty()) {
            Evidence latest=newestFirst.get(0);result.setLatestRating(latest.getRating());
            result.setLastEvidenceAt(latest.getCreatedAt());result.setLatestEvidenceCardVersion(latest.getCardContentVersion());
        }
        return result;
    }

    private boolean eligible(Long accountId,LearningContentAccess.ChapterRef chapter) {
        Set<Long> initial=new HashSet<>(mapper.initialCardIds(accountId,chapter.getChapterId()));
        // Zero-card content can be read, but is not evidence-based initial study.
        if(chapter.getSnapshot().path("cards").isEmpty()) return false;
        for(JsonNode card:chapter.getSnapshot().path("cards")) if(!initial.contains(card.path("id").asLong())) return false;
        Set<Long> answered=new HashSet<>(mapper.answeredQuestionIds(accountId));
        for(JsonNode question:chapter.getSnapshot().path("questions")) if(!answered.contains(question.path("id").asLong())) return false;
        return true;
    }

    @Transactional
    public void completeChapter(Long chapterId) {
        Long accountId=actor();lock(accountId);LearningContentAccess.ChapterRef chapter=content.chapter(chapterId);
        if(!eligible(accountId,chapter)) throw conflict("请完成本章全部知识卡片评价和问题的首次回答");
        reconcile(accountId,chapter);
    }

    private void reconcile(Long accountId,LearningContentAccess.ChapterRef chapter) {
        if(!eligible(accountId,chapter)) return;
        progress.ensureRow(accountId,chapter.getTutorialId(),chapter.getGroupId(),chapter.getChapterId());
        if(progress.markCompleted(accountId,chapter.getChapterId())==1) events.chapter(accountId,"CHAPTER_COMPLETED",chapter);
        for (Session session : mapper.openInitialSessions(accountId,chapter.getChapterId())) {
            if (session.getStudyPlanId() == null || roundEligible(accountId,session,chapter)) finish(accountId,session);
        }
        mapper.reconcileTasks(accountId);mapper.reconcilePlans(accountId);
    }

    private boolean roundEligible(Long accountId,Session session,LearningContentAccess.ChapterRef chapter) {
        List<Item> items = mapper.items(accountId,session.getId());
        if (items.isEmpty() || items.stream().anyMatch(i -> !"COMPLETED".equals(i.getStatus()))) return false;
        Set<Long> rated = new HashSet<>(items.stream().map(Item::getKnowledgeCardId).toList());
        for (JsonNode card : chapter.getSnapshot().path("cards")) if (!rated.contains(card.path("id").asLong())) return false;
        Set<Long> answered = new HashSet<>(mapper.answeredForSession(accountId,session.getId()));
        for (JsonNode question : chapter.getSnapshot().path("questions")) if (!answered.contains(question.path("id").asLong())) return false;
        return true;
    }

    private void finish(Long accountId,Session session) {
        if(mapper.sessionStatus(accountId,session.getId(),"COMPLETED")==1) {
            mapper.completePlanChapter(accountId,session.getId());
            SessionSummary completed = summary(accountId,session);
            try { mapper.saveSessionSummary(accountId,session.getId(),json.writeValueAsString(completed)); }
            catch (JsonProcessingException exception) { throw new IllegalStateException("学习摘要保存失败",exception); }
            events.session(accountId,session,completed);
        }
    }

    public Answer answer(Long questionId) {
        content.question(questionId);Answer answer=mapper.answer(actor(),questionId);
        if(answer!=null) answer.setVersions(mapper.answerVersions(actor(),questionId));return answer;
    }

    public List<AnswerVersion> versions(Long questionId) { content.question(questionId);return mapper.answerVersions(actor(),questionId); }

    @Transactional
    public Answer saveVersion(Long questionId,AnswerVersionDTO request) {
        Long accountId=actor();lock(accountId);LearningContentAccess.QuestionRef question=content.question(questionId);
        if(request.getAnswerText()==null || request.getAnswerText().isBlank() || request.getAnswerText().length()>100000) throw new LearningInvalidRequestException("请填写不超过 100000 字的回答");
        if(!Set.of("BEFORE_REFERENCE","AFTER_REFERENCE").contains(request.getAnswerPhase())) throw new LearningInvalidRequestException("回答阶段无效");
        Answer answer=mapper.answer(accountId,questionId);
        if("AFTER_REFERENCE".equals(request.getAnswerPhase()) && (answer==null || answer.getReferenceUnlockedAt()==null)) throw new LearningAnswerLockedException();
        if(answer==null) { answer=new Answer();answer.setAccountId(accountId);answer.setQuestionId(questionId);answer.setVersionCount(0);mapper.insertAnswer(answer); }
        AnswerVersion version=new AnswerVersion();version.setAnswerId(answer.getId());version.setVersionNo(answer.getVersionCount()+1);
        version.setAnswerText(request.getAnswerText());version.setAnswerPhase(request.getAnswerPhase());mapper.insertAnswerVersion(version);
        answer.setLatestVersionId(version.getId());answer.setVersionCount(version.getVersionNo());answer.setAnswerText(version.getAnswerText());
        mapper.updateAnswerHeader(answer);reconcile(accountId,question.getChapter());return answer(questionId);
    }

    public String reference(Long questionId) {
        LearningContentAccess.QuestionRef question=content.question(questionId);Answer answer=mapper.answer(actor(),questionId);
        if(answer==null || answer.getReferenceUnlockedAt()==null
                || mapper.answerVersions(actor(),questionId).stream().noneMatch(v->"BEFORE_REFERENCE".equals(v.getAnswerPhase()))) throw new LearningAnswerLockedException();
        return question.getReferenceAnswer();
    }

    public List<CardMastery> mastery(Long tutorialId,Long chapterId) {
        Long accountId=actor();Map<Long,Mastery> saved=new HashMap<>();mapper.masteries(accountId).forEach(m->saved.put(m.getKnowledgeCardId(),m));
        List<CardMastery> result=new ArrayList<>();
        if(tutorialId!=null) content.publishedTutorial(tutorialId);
        if(chapterId!=null) content.chapter(chapterId);
        for(var tutorial:tutorials.listPublished()) {
            if(tutorialId!=null && !tutorialId.equals(tutorial.getId())) continue;
            JsonNode snapshot=content.publishedTutorial(tutorial.getId());int chapterOrder=0;
            for(JsonNode group:snapshot.path("groups")) for(JsonNode chapter:group.path("chapters")) {
                int order=chapterOrder++;if(chapterId!=null && chapter.path("id").asLong()!=chapterId) continue;
                int cardOrder=0;
                for(JsonNode node:chapter.path("cards")) {
                    CardMastery card=new CardMastery();card.setKnowledgeCardId(node.path("id").asLong());
                    card.setTutorialId(tutorial.getId());card.setChapterId(chapter.path("id").asLong());
                    card.setTutorialTitle(snapshot.path("title").asText());card.setChapterTitle(chapter.path("title").asText());
                    card.setFrontText(node.path("frontText").asText());card.setBackMarkdown(node.path("backMarkdown").asText());
                    card.setContentVersion(node.path("contentVersion").asInt(1));card.setChapterOrder(order);card.setCardOrder(cardOrder++);
                    Mastery row=saved.get(card.getKnowledgeCardId());
                    card.setMasteryStatus(row==null?"UNLEARNED":row.getMasteryStatus());card.setLatestRating(row==null?null:row.getLatestRating());
                    card.setEvidenceCount(row==null?0:row.getEvidenceCount());card.setRecentScore(row==null?0:row.getRecentScore());
                    card.setLastEvidenceAt(row==null?null:row.getLastEvidenceAt());
                    card.setNeedsRevalidation(row!=null && row.getEvidenceCount()>0 && row.getLatestEvidenceCardVersion()!=null && row.getLatestEvidenceCardVersion()<card.getContentVersion());
                    card.setPriority(card.getEvidenceCount()==0?null:EvidenceAlgorithms.priority(card));result.add(card);
                }
            }
        }
        return result;
    }

    private List<CardMastery> candidates() { return mastery(null,null).stream().filter(c->c.getEvidenceCount()>0).toList(); }

    public PageResult<CardMastery> masteryPage(LearningPageQueryDTO query) {
        query.validate(Set.of("UNLEARNED", "LEARNING", "BASIC_MASTERED", "STABLE_MASTERED"));
        String keyword = query.getKeyword() == null ? "" : query.getKeyword().strip().toLowerCase(Locale.ROOT);
        // Current published snapshots are the authority, including cards with no evidence yet.
        List<CardMastery> rows = mastery(query.getTutorialId(), null).stream()
                .filter(c -> query.getStatus() == null || query.getStatus().isBlank() || query.getStatus().equals(c.getMasteryStatus()))
                .filter(c -> query.getNeedsRevalidation() == null || query.getNeedsRevalidation().equals(c.getNeedsRevalidation()))
                .filter(c -> (c.getFrontText() + " " + c.getChapterTitle() + " " + c.getTutorialTitle()).toLowerCase(Locale.ROOT).contains(keyword)).toList();
        return PageResult.<CardMastery>builder().items(rows.stream().skip(query.getOffset()).limit(query.getPageSize()).toList())
                .total(rows.size()).page(query.getPage()).pageSize(query.getPageSize()).build();
    }

    public List<TutorialOption> masteryOptions() {
        return mastery(null, null).stream().collect(java.util.stream.Collectors.toMap(CardMastery::getTutorialId, c -> {
            TutorialOption option = new TutorialOption();option.setId(c.getTutorialId());option.setTitle(c.getTutorialTitle());return option;
        }, (first, duplicate) -> first, LinkedHashMap::new)).values().stream().toList();
    }

    public List<TutorialOption> historyOptions() { return mapper.historyTutorials(actor()); }

    public ReviewSummary reviewSummary() {
        List<CardMastery> cards=candidates();ReviewSummary result=new ReviewSummary();result.setCandidateCount(cards.size());
        result.setRecommendedCount((int)cards.stream().filter(c->c.getPriority()<5).count());result.setStableCount(cards.size()-result.getRecommendedCount());
        Map<String,Integer> buckets=new LinkedHashMap<>();for(int i=0;i<6;i++) buckets.put("P"+i,0);
        cards.forEach(c->buckets.merge("P"+c.getPriority(),1,Integer::sum));result.setPriorities(buckets);return result;
    }

    public Session currentReview() { Session current=mapper.currentReview(actor());return current==null?null:sessionView(actor(),current); }

    @Transactional
    public Session review(ReviewSessionDTO request) {
        Long accountId=actor();lock(accountId);Session current=mapper.currentReview(accountId);
        if(current!=null) throw conflict("你有一组未完成复习，请继续或放弃后重新开始");
        boolean audit="STABLE_AUDIT".equals(request.getMode());
        if(!audit && !"RECOMMENDED".equals(request.getMode())) throw new LearningInvalidRequestException("复习模式无效");
        if(request.getCount()==null || request.getCount()<1 || request.getCount()>100) throw new LearningInvalidRequestException("复习数量必须在 1～100 之间");
        List<CardMastery> selected=EvidenceAlgorithms.select(candidates(),request.getCount(),audit);
        if(selected.isEmpty()) throw conflict(audit?"暂无稳定掌握的知识可供抽查":"完成首次学习后，知识会进入复习");
        Session session=new Session();session.setAccountId(accountId);session.setSessionType(audit?"STABLE_AUDIT":"REVIEW");
        session.setStatus("IN_PROGRESS");session.setRequestedItemCount(request.getCount());mapper.insertSession(session);
        int sequence=0;
        for(CardMastery card:selected) {
            Item item=new Item();item.setSessionId(session.getId());item.setAccountId(accountId);
            item.setTutorialId(card.getTutorialId());item.setChapterId(card.getChapterId());item.setKnowledgeCardId(card.getKnowledgeCardId());
            item.setCardContentVersion(card.getContentVersion());item.setFrontText(card.getFrontText());item.setBackMarkdown(card.getBackMarkdown());
            item.setSourcePriority(card.getPriority());item.setSequenceNo(++sequence);mapper.insertItem(item);
        }
        return sessionView(accountId,session);
    }

    @Transactional
    public Session abandon(Long id) {
        Long accountId=actor();lock(accountId);Session session=requireSession(accountId,id);
        if("INITIAL_STUDY".equals(session.getSessionType())) throw conflict("首次学习保留当前进度，请直接继续");
        mapper.sessionStatus(accountId,id,"ABANDONED");return sessionView(accountId,requireSession(accountId,id));
    }

    private SessionSummary summary(Long accountId,Session session) {
        if(session.getSummaryJson()!=null) {
            try { return json.readValue(session.getSummaryJson(),SessionSummary.class); }
            catch (JsonProcessingException exception) { throw new IllegalStateException("学习摘要读取失败",exception); }
        }
        List<Evidence> evidence=mapper.sessionEvidence(accountId,session.getId());SessionSummary summary=new SessionSummary();
        summary.setCardCount(evidence.size());summary.setForgotCount(0);summary.setFuzzyCount(0);summary.setRememberedCount(0);
        summary.setRevalidationCount(0);summary.setUpgradedCount(0);summary.setDowngradedCount(0);
        Map<String,Integer> transitions=new LinkedHashMap<>();
        for(Evidence row:evidence) {
            switch(row.getRating()) {
                case "FORGOT" -> summary.setForgotCount(summary.getForgotCount()+1);
                case "FUZZY" -> summary.setFuzzyCount(summary.getFuzzyCount()+1);
                case "REMEMBERED" -> summary.setRememberedCount(summary.getRememberedCount()+1);
                default -> throw new IllegalStateException("Invalid persisted evidence rating");
            }
            if("REVALIDATION".equals(row.getEvidenceType())) summary.setRevalidationCount(summary.getRevalidationCount()+1);
            if(row.getPreviousMasteryStatus()!=null && !row.getPreviousMasteryStatus().equals(row.getMasteryStatus())) {
                transitions.merge(row.getPreviousMasteryStatus()+"→"+row.getMasteryStatus(),1,Integer::sum);
                if(EvidenceAlgorithms.level(row.getMasteryStatus())>EvidenceAlgorithms.level(row.getPreviousMasteryStatus())) summary.setUpgradedCount(summary.getUpgradedCount()+1);
                else summary.setDowngradedCount(summary.getDowngradedCount()+1);
            }
        }
        summary.setTransitions(transitions);summary.setQuestionCount(0);
        if(session.getChapterId()!=null) {
            try {
                Set<Long> answered=new HashSet<>(session.getStudyPlanId() == null ? mapper.answeredQuestionIds(accountId) : mapper.answeredForSession(accountId,session.getId()));
                for(JsonNode question:content.chapter(session.getChapterId()).getSnapshot().path("questions")) if(answered.contains(question.path("id").asLong())) summary.setQuestionCount(summary.getQuestionCount()+1);
            } catch(LearningResourceNotFoundException ignored) { /* Withdrawn content retains evidence history. */ }
        }
        return summary;
    }

    public PageResult<Session> history(int page,int pageSize) {
        if(page<1 || pageSize<1 || pageSize>100 || (long)(page-1)*pageSize>Integer.MAX_VALUE) throw new LearningInvalidRequestException("分页参数无效");
        Long accountId=actor();List<Session> sessions=mapper.historySessions(accountId,(page-1)*pageSize,pageSize);
        sessions.forEach(s->s.setSummary(summary(accountId,s)));
        return PageResult.<Session>builder().items(sessions).total(mapper.countHistorySessions(accountId)).page(page).pageSize(pageSize).build();
    }

    public PageResult<Session> history(LearningPageQueryDTO query) {
        query.validate(Set.of("IN_PROGRESS", "COMPLETED", "ABANDONED"));
        Long accountId = actor();
        List<Session> rows = mapper.filteredHistory(accountId, query);
        rows.forEach(s -> s.setSummary(summary(accountId, s)));
        return PageResult.<Session>builder().items(rows).total(mapper.countFilteredHistory(accountId, query))
                .page(query.getPage()).pageSize(query.getPageSize()).build();
    }

    public Statistics statistics() {
        List<CardMastery> cards=mastery(null,null);Statistics result=new Statistics();result.setPlans(plans.list());
        result.setUnfinishedPlanCount((int)result.getPlans().stream().filter(p->Set.of("DRAFT","ACTIVE","PAUSED").contains(p.getStatus())).count());
        result.setLearnedCardCount((int)cards.stream().filter(c->c.getEvidenceCount()>0).count());
        result.setLearningCount((int)cards.stream().filter(c->"LEARNING".equals(c.getMasteryStatus())).count());
        result.setBasicMasteredCount((int)cards.stream().filter(c->"BASIC_MASTERED".equals(c.getMasteryStatus())).count());
        result.setStableMasteredCount((int)cards.stream().filter(c->"STABLE_MASTERED".equals(c.getMasteryStatus())).count());
        result.setRecommendedCount((int)cards.stream().filter(c->c.getPriority()!=null && c.getPriority()<5).count());return result;
    }
}
