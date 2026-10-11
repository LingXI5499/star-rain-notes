/*
 * EvidenceAlgorithmsTest：教程与学习模块的测试文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 阅读入口：masteryDependsOnCountAndRecentThree（掌握Depends触发数量及RecentThree处理入口）；chapter（章节处理入口）；documentExampleKeepsGroupsAndLargeChaptersIntact（文档Example保留Groups及Large章节列表Intact处理入口）；missingChapterBreaksTheBlockAndOversizedChapterStandsAlone（缺失章节BreaksThe区块及Oversized章节StandsAlone处理入口）。
 */

package com.starrainnotes.tutorial.learning.service;

import static org.junit.jupiter.api.Assertions.*;
import com.starrainnotes.tutorial.learning.entity.EvidenceModels.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 证据Algorithms测试，属于教程与学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class EvidenceAlgorithmsTest {
    /**
     * 掌握Depends触发数量及RecentThree处理入口。
     * 关键处理步骤调用 <code>EvidenceAlgorithms.mastery</code>。
     *
     * @param count 数量；类型为 <code>int</code>
     * @param score score；类型为 <code>int</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     */
    @ParameterizedTest
    @CsvSource({"0,0,UNLEARNED","1,2,LEARNING","2,4,LEARNING","3,6,STABLE_MASTERED",
            "3,5,BASIC_MASTERED","3,4,BASIC_MASTERED","3,3,LEARNING","3,2,LEARNING","3,0,LEARNING",
            "100,6,STABLE_MASTERED","100,4,BASIC_MASTERED","100,2,LEARNING"})
    void masteryDependsOnCountAndRecentThree(int count,int score,String status) {
        assertEquals(status,EvidenceAlgorithms.mastery(count,score));
    }

    /**
     * 章节处理入口。
     * 关键处理步骤调用 <code>chapter.setChapterId</code>、<code>chapter.setGroupId</code>、<code>chapter.setGroupOrder</code>、<code>chapter.setChapterOrder</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param group 分组；类型为 <code>long</code>
     * @param order 顺序；类型为 <code>int</code>
     * @param cards 卡片集合；类型为 <code>int</code>
     * @return 类型为 <code>Chapter</code> 的处理结果
     */
    private Chapter chapter(long id,long group,int order,int cards) {
        Chapter chapter=new Chapter();chapter.setChapterId(id);chapter.setGroupId(group);chapter.setGroupOrder((int)group);
        chapter.setChapterOrder(order);chapter.setCardCount(cards);chapter.setQuestionCount(0);chapter.setTutorialId(1L);return chapter;
    }

    /**
     * 文档Example保留Groups及Large章节列表Intact处理入口。
     * 关键处理步骤调用 <code>List.of</code>、<code>chapter</code>、<code>EvidenceAlgorithms.tasks</code>、<code>tasks.stream</code>。
     */
    @Test void documentExampleKeepsGroupsAndLargeChaptersIntact() {
        List<Chapter> chapters=List.of(chapter(1,1,0,7),chapter(2,1,1,5),chapter(3,1,2,3),
                chapter(4,2,0,6),chapter(5,2,1,7),chapter(6,3,0,14),chapter(7,3,1,7),chapter(8,3,2,12));
        List<Task> tasks=EvidenceAlgorithms.tasks(chapters,12);
        assertEquals(List.of(12,3,13,14,7,12),tasks.stream().map(Task::getCardCount).toList());
        assertEquals(List.of(1L,2L),tasks.get(0).getChapters().stream().map(Chapter::getChapterId).toList());
    }

    /**
     * 缺失章节BreaksThe区块及Oversized章节StandsAlone处理入口。
     * 关键处理步骤调用 <code>EvidenceAlgorithms.tasks</code>、<code>List.of</code>、<code>chapter</code>、<code>tasks.stream</code>。
     */
    @Test void missingChapterBreaksTheBlockAndOversizedChapterStandsAlone() {
        List<Task> tasks=EvidenceAlgorithms.tasks(List.of(chapter(1,1,0,3),chapter(3,1,2,3),chapter(4,1,3,36),chapter(5,1,4,3)),12);
        assertEquals(List.of(3,3,36,3),tasks.stream().map(Task::getCardCount).toList());
    }

    /**
     * equallyCloseCombines及Input顺序Does不Change预览处理入口。
     * 关键处理步骤调用 <code>List.of</code>、<code>chapter</code>、<code>EvidenceAlgorithms.tasks</code>。
     */
    @Test void equallyCloseCombinesAndInputOrderDoesNotChangePreview() {
        List<Chapter> chapters=List.of(chapter(2,1,1,8),chapter(1,1,0,8));
        assertEquals(16,EvidenceAlgorithms.tasks(chapters,12).get(0).getCardCount());
        assertEquals(1L,EvidenceAlgorithms.tasks(chapters,12).get(0).getChapters().get(0).getChapterId());
    }

    /**
     * 卡片处理入口。
     * 关键处理步骤调用 <code>card.setKnowledgeCardId</code>、<code>card.setTutorialId</code>、<code>card.setEvidenceCount</code>、<code>card.setLastEvidenceAt</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param tutorial 教程；类型为 <code>long</code>
     * @param priority priority；类型为 <code>int</code>
     * @param age age；类型为 <code>int</code>
     * @return 类型为 <code>CardMastery</code> 的处理结果
     */
    private CardMastery card(long id,long tutorial,int priority,int age) {
        CardMastery card=new CardMastery();card.setKnowledgeCardId(id);card.setTutorialId(tutorial);
        card.setEvidenceCount(3);card.setLastEvidenceAt(LocalDateTime.of(2026,1,1,0,0).plusMinutes(age));
        card.setChapterOrder(0);card.setCardOrder(0);card.setNeedsRevalidation(priority==0);
        card.setLatestRating(priority==1?"FORGOT":priority==2?"FUZZY":"REMEMBERED");
        card.setMasteryStatus(priority==3?"LEARNING":priority==4?"BASIC_MASTERED":"STABLE_MASTERED");return card;
    }

    /**
     * priorityWinsOverDiversity及Stable卡片集合OnlyFillShortage处理入口。
     * 关键处理步骤调用 <code>List.of</code>、<code>card</code>、<code>EvidenceAlgorithms.select</code>。
     */
    @Test void priorityWinsOverDiversityAndStableCardsOnlyFillShortage() {
        List<CardMastery> cards=List.of(card(1,1,0,5),card(2,1,1,2),card(3,2,4,0),card(4,3,5,-10));
        assertEquals(List.of(1L,2L,3L),EvidenceAlgorithms.select(cards,3,false).stream().map(CardMastery::getKnowledgeCardId).toList());
        assertEquals(List.of(1L,2L,3L,4L),EvidenceAlgorithms.select(cards,10,false).stream().map(CardMastery::getKnowledgeCardId).toList());
        assertEquals(List.of(4L),EvidenceAlgorithms.select(cards,10,true).stream().map(CardMastery::getKnowledgeCardId).toList());
    }

    /**
     * samePriorityRoundRobinsTutorials来源OldestHead处理入口。
     * 关键处理步骤调用 <code>List.of</code>、<code>card</code>、<code>EvidenceAlgorithms.select</code>。
     */
    @Test void samePriorityRoundRobinsTutorialsFromOldestHead() {
        List<CardMastery> cards=List.of(card(1,1,3,0),card(2,1,3,1),card(3,1,3,2),card(4,2,3,3),card(5,2,3,4));
        assertEquals(List.of(1L,4L,2L,5L,3L),EvidenceAlgorithms.select(cards,10,false).stream().map(CardMastery::getKnowledgeCardId).toList());
    }

    /**
     * 内容修订PrecedesForgotten及Does不Reset掌握处理入口。
     * 关键处理步骤调用 <code>card</code>、<code>card.setNeedsRevalidation</code>、<code>EvidenceAlgorithms.priority</code>、<code>card.getMasteryStatus</code>。
     */
    @Test void contentRevisionPrecedesForgottenAndDoesNotResetMastery() {
        CardMastery card=card(1,1,1,0);card.setNeedsRevalidation(true);
        assertEquals(0,EvidenceAlgorithms.priority(card));assertEquals("STABLE_MASTERED",card.getMasteryStatus());
    }

    /**
     * 最新ThreeCanDowngrade及时间是否Absent来源掌握Inputs处理入口。
     * 关键处理步骤调用 <code>List.of</code>、<code>status</code>、<code>ratings.add</code>。
     */
    @Test void latestThreeCanDowngradeAndTimeIsAbsentFromMasteryInputs() {
        List<String> ratings=new ArrayList<>(List.of("REMEMBERED","REMEMBERED","REMEMBERED"));
        assertEquals("STABLE_MASTERED",status(ratings));ratings.add("FORGOT");assertEquals("BASIC_MASTERED",status(ratings));
        ratings.add("FORGOT");assertEquals("LEARNING",status(ratings));
    }
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束处理入口。
     * 关键处理步骤调用 <code>EvidenceAlgorithms.mastery</code>、<code>ratings.size</code>、<code>ratings.subList</code>、<code>Math.max</code>。
     *
     * @param ratings ratings；类型为 <code>List&lt;String&gt;</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String status(List<String> ratings) { return EvidenceAlgorithms.mastery(ratings.size(),ratings.subList(Math.max(0,ratings.size()-3),ratings.size()).stream().mapToInt(EvidenceAlgorithms::score).sum()); }
}
