package com.starrainnotes.message;

import com.starrainnotes.account.api.AccountReferenceApi;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.message.dto.MessageSubmitDTO;
import com.starrainnotes.message.entity.MessageActionEntity;
import com.starrainnotes.message.entity.MessageEntity;
import com.starrainnotes.message.mapper.MessageActionMapper;
import com.starrainnotes.message.mapper.MessageMapper;
import com.starrainnotes.message.service.impl.MessageServiceImpl;
import com.starrainnotes.message.service.MessageSubmissionLimiter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {
    @Mock private MessageMapper messages;
    @Mock private MessageActionMapper actions;
    @Mock private CurrentActorApi actor;
    @Mock private AccountReferenceApi accounts;
    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MessageServiceImpl(messages, actions, actor, accounts, new MessageSubmissionLimiter());
    }

    @Test
    void publicProjectionNeverExposesPrivateFields() {
        MessageEntity entity = new MessageEntity();
        entity.setId(7L);
        entity.setAccountId(99L);
        entity.setAuthorDisplayName("访客");
        entity.setContactEmail("private@example.com");
        entity.setContent("你好");
        entity.setStatus("PUBLIC");
        entity.setSubmittedAt(LocalDateTime.now());
        when(messages.countByStatus("PUBLIC")).thenReturn(1L);
        when(messages.pageByStatus(eq("PUBLIC"), eq(20), eq(0L))).thenReturn(List.of(entity));

        var item = service.publicMessages(1, 20).getItems().get(0);

        assertEquals("访客", item.getAuthorDisplayName());
        assertEquals("你好", item.getContent());
        assertEquals(4, item.getClass().getDeclaredFields().length);
    }

    @Test
    void onlyOnePendingDecisionCanCreateAnAction() {
        when(actor.current()).thenReturn(new CurrentActorApi.CurrentActor(1L, null, null));
        when(messages.decide(eq(8L), eq("PUBLIC"), eq(1L), any(), eq(null))).thenReturn(0);
        MessageEntity alreadyApproved = new MessageEntity();
        alreadyApproved.setStatus("PUBLIC");
        when(messages.selectById(8L)).thenReturn(alreadyApproved);

        ApiException error = assertThrows(ApiException.class, () -> service.approve(8L));

        assertEquals("MESSAGE_ALREADY_MODERATED", error.getCode());
        verify(actions, never()).insert(any(MessageActionEntity.class));
    }

    @Test
    void loggedInSubmissionUsesAccountDisplayName() {
        MessageSubmitDTO request = new MessageSubmitDTO();
        request.setAuthorDisplayName("forged name");
        request.setContent("hello");
        when(actor.currentOptional()).thenReturn(Optional.of(new CurrentActorApi.CurrentActor(2L, null, null)));
        when(accounts.displayName(2L)).thenReturn("实际昵称");

        service.submit(request, "127.0.0.1");

        org.mockito.ArgumentCaptor<MessageEntity> saved = org.mockito.ArgumentCaptor.forClass(MessageEntity.class);
        verify(messages).insert(saved.capture());
        assertEquals("实际昵称", saved.getValue().getAuthorDisplayName());
        assertEquals(2L, saved.getValue().getAccountId());
    }

    @Test
    void submissionIsRateLimited() {
        MessageSubmissionLimiter limiter = new MessageSubmissionLimiter();
        for (int index = 0; index < 5; index++) {
            limiter.check("192.0.2.4");
        }
        ApiException error = assertThrows(ApiException.class, () -> limiter.check("192.0.2.4"));
        assertEquals("MESSAGE_RATE_LIMITED", error.getCode());
        assertTrue(error.getStatus() == 429);
    }
}
