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

class LearningAnswerServiceImplTest {
    private CurrentActorApi currentActor;
    private LearningContentAccess content;
    private UserQuestionAnswerMapper mapper;
    private LearningEventWriter events;
    private LearningAnswerServiceImpl service;

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

    @Test
    void referenceAnswerRequiresThisAccountsSubmission() {
        when(mapper.selectByAccountIdAndQuestionId(42L, 9L)).thenReturn(null);
        assertThrows(LearningAnswerLockedException.class, () -> service.referenceAnswer(9L));
    }

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
