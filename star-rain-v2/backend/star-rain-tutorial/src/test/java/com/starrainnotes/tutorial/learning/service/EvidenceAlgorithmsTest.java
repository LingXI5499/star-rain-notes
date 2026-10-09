package com.starrainnotes.tutorial.learning.service;

import static org.junit.jupiter.api.Assertions.*;
import com.starrainnotes.tutorial.learning.entity.EvidenceModels.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EvidenceAlgorithmsTest {
    @ParameterizedTest
    @CsvSource({"0,0,UNLEARNED","1,2,LEARNING","2,4,LEARNING","3,6,STABLE_MASTERED",
            "3,5,BASIC_MASTERED","3,4,BASIC_MASTERED","3,3,LEARNING","3,2,LEARNING","3,0,LEARNING",
            "100,6,STABLE_MASTERED","100,4,BASIC_MASTERED","100,2,LEARNING"})
    void masteryDependsOnCountAndRecentThree(int count,int score,String status) {
        assertEquals(status,EvidenceAlgorithms.mastery(count,score));
    }

    private Chapter chapter(long id,long group,int order,int cards) {
        Chapter chapter=new Chapter();chapter.setChapterId(id);chapter.setGroupId(group);chapter.setGroupOrder((int)group);
        chapter.setChapterOrder(order);chapter.setCardCount(cards);chapter.setQuestionCount(0);chapter.setTutorialId(1L);return chapter;
    }

    @Test void documentExampleKeepsGroupsAndLargeChaptersIntact() {
        List<Chapter> chapters=List.of(chapter(1,1,0,7),chapter(2,1,1,5),chapter(3,1,2,3),
                chapter(4,2,0,6),chapter(5,2,1,7),chapter(6,3,0,14),chapter(7,3,1,7),chapter(8,3,2,12));
        List<Task> tasks=EvidenceAlgorithms.tasks(chapters,12);
        assertEquals(List.of(12,3,13,14,7,12),tasks.stream().map(Task::getCardCount).toList());
        assertEquals(List.of(1L,2L),tasks.get(0).getChapters().stream().map(Chapter::getChapterId).toList());
    }

    @Test void missingChapterBreaksTheBlockAndOversizedChapterStandsAlone() {
        List<Task> tasks=EvidenceAlgorithms.tasks(List.of(chapter(1,1,0,3),chapter(3,1,2,3),chapter(4,1,3,36),chapter(5,1,4,3)),12);
        assertEquals(List.of(3,3,36,3),tasks.stream().map(Task::getCardCount).toList());
    }

    @Test void equallyCloseCombinesAndInputOrderDoesNotChangePreview() {
        List<Chapter> chapters=List.of(chapter(2,1,1,8),chapter(1,1,0,8));
        assertEquals(16,EvidenceAlgorithms.tasks(chapters,12).get(0).getCardCount());
        assertEquals(1L,EvidenceAlgorithms.tasks(chapters,12).get(0).getChapters().get(0).getChapterId());
    }

    private CardMastery card(long id,long tutorial,int priority,int age) {
        CardMastery card=new CardMastery();card.setKnowledgeCardId(id);card.setTutorialId(tutorial);
        card.setEvidenceCount(3);card.setLastEvidenceAt(LocalDateTime.of(2026,1,1,0,0).plusMinutes(age));
        card.setChapterOrder(0);card.setCardOrder(0);card.setNeedsRevalidation(priority==0);
        card.setLatestRating(priority==1?"FORGOT":priority==2?"FUZZY":"REMEMBERED");
        card.setMasteryStatus(priority==3?"LEARNING":priority==4?"BASIC_MASTERED":"STABLE_MASTERED");return card;
    }

    @Test void priorityWinsOverDiversityAndStableCardsOnlyFillShortage() {
        List<CardMastery> cards=List.of(card(1,1,0,5),card(2,1,1,2),card(3,2,4,0),card(4,3,5,-10));
        assertEquals(List.of(1L,2L,3L),EvidenceAlgorithms.select(cards,3,false).stream().map(CardMastery::getKnowledgeCardId).toList());
        assertEquals(List.of(1L,2L,3L,4L),EvidenceAlgorithms.select(cards,10,false).stream().map(CardMastery::getKnowledgeCardId).toList());
        assertEquals(List.of(4L),EvidenceAlgorithms.select(cards,10,true).stream().map(CardMastery::getKnowledgeCardId).toList());
    }

    @Test void samePriorityRoundRobinsTutorialsFromOldestHead() {
        List<CardMastery> cards=List.of(card(1,1,3,0),card(2,1,3,1),card(3,1,3,2),card(4,2,3,3),card(5,2,3,4));
        assertEquals(List.of(1L,4L,2L,5L,3L),EvidenceAlgorithms.select(cards,10,false).stream().map(CardMastery::getKnowledgeCardId).toList());
    }

    @Test void contentRevisionPrecedesForgottenAndDoesNotResetMastery() {
        CardMastery card=card(1,1,1,0);card.setNeedsRevalidation(true);
        assertEquals(0,EvidenceAlgorithms.priority(card));assertEquals("STABLE_MASTERED",card.getMasteryStatus());
    }

    @Test void latestThreeCanDowngradeAndTimeIsAbsentFromMasteryInputs() {
        List<String> ratings=new ArrayList<>(List.of("REMEMBERED","REMEMBERED","REMEMBERED"));
        assertEquals("STABLE_MASTERED",status(ratings));ratings.add("FORGOT");assertEquals("BASIC_MASTERED",status(ratings));
        ratings.add("FORGOT");assertEquals("LEARNING",status(ratings));
    }
    private String status(List<String> ratings) { return EvidenceAlgorithms.mastery(ratings.size(),ratings.subList(Math.max(0,ratings.size()-3),ratings.size()).stream().mapToInt(EvidenceAlgorithms::score).sum()); }
}
