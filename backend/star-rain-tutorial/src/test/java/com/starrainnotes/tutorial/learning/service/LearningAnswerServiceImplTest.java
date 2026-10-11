/*
 * LearningAnswerServiceImplTest：教程与学习模块的测试文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：CurrentActorApi、LearningAnswerDTO、UserQuestionAnswerEntity、LearningAnswerLockedException、UserQuestionAnswerMapper、LearningAnswerServiceImpl、LearningContentAccess。
 * 阅读入口：setUp（初始化当前测试场景的被测对象与模拟依赖）；referenceAnswerRequiresThisAccountsSubmission（引用答案RequiresThis账号集合Submission处理入口）；submittedAnswerUnlocksReferenceAndUsesCurrentAccount（提交答案Unlocks引用及Uses当前账号处理入口）。
 */

package com.starrainnotes.tutorial.learning.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.tutorial.learning.dto.LearningAnswerDTO;
import com.starrainnotes.tutorial.learning.entity.UserQuestionAnswerEntity;
import com.starrainnotes.tutorial.learning.exception.LearningAnswerLockedException;
import com.starrainnotes.tutorial.learning.mapper.UserQuestionAnswerMapper;
import com.starrainnotes.tutorial.learning.service.impl.LearningAnswerServiceImpl;
import com.starrainnotes.tutorial.learning.service.impl.LearningContentAccess;
import com.starrainnotes.tutorial.learning.service.impl.LearningEventWriter;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 学习答案业务服务实现测试，属于教程与学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class LearningAnswerServiceImplTest {
    /**
     * 当前操作者。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private CurrentActorApi currentActor;
    /**
     * 内容。
     */
    private LearningContentAccess content;
    /**
     * 数据映射。
     * 协作类型：<code>UserQuestionAnswerMapper</code>。
     */
    private UserQuestionAnswerMapper mapper;
    /**
     * 事件集合。
     */
    private LearningEventWriter events;
    /**
     * 业务服务。
     */
    private LearningAnswerServiceImpl service;

    /**
     * 初始化当前测试场景的被测对象与模拟依赖。
     * 主要协作调用：<code>currentActor.current</code>、<code>content.question</code>。
     */
    @BeforeEach
    void setUp() {
        currentActor = mock(CurrentActorApi.class);
        content = mock(LearningContentAccess.class);
        mapper = mock(UserQuestionAnswerMapper.class);
        events = mock(LearningEventWriter.class);
        service = new LearningAnswerServiceImpl(currentActor, content, mapper, events);
        when(currentActor.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(42L).build());
        when(content.question(9L)).thenReturn(LearningContentAccess.QuestionRef.builder()
                .questionId(9L).referenceAnswer("正确答案")
                .chapter(LearningContentAccess.ChapterRef.builder().tutorialId(1L).chapterId(2L).build())
                .build());
    }

    /**
     * 引用答案RequiresThis账号集合Submission处理入口。
     * 主要协作调用：<code>mapper.selectByAccountIdAndQuestionId</code>、<code>service.referenceAnswer</code>。
     */
    @Test
    void referenceAnswerRequiresThisAccountsSubmission() {
        when(mapper.selectByAccountIdAndQuestionId(42L, 9L)).thenReturn(null);
        assertThrows(LearningAnswerLockedException.class, () -> service.referenceAnswer(9L));
    }

    /**
     * 提交答案Unlocks引用及Uses当前账号处理入口。
     * 主要协作调用：<code>mapper.selectByAccountIdAndQuestionId</code>、<code>service.referenceAnswer</code>、<code>service.answer</code>。
     */
    @Test
    void submittedAnswerUnlocksReferenceAndUsesCurrentAccount() {
        UserQuestionAnswerEntity saved = new UserQuestionAnswerEntity();
        saved.setAccountId(42L);
        saved.setQuestionId(9L);
        saved.setAnswerText("我的答案");
        saved.setReferenceUnlockedAt(LocalDateTime.now());
        when(mapper.selectByAccountIdAndQuestionId(42L, 9L)).thenReturn(saved);
        assertEquals("正确答案", service.referenceAnswer(9L));
        LearningAnswerDTO request = new LearningAnswerDTO();
        request.setAnswerText(" 我的答案 ");
        assertEquals("我的答案", service.answer(9L, request).getAnswerText());
        verify(mapper).upsertAnswer(eq(42L), eq(9L), eq("我的答案"));
    }
}
