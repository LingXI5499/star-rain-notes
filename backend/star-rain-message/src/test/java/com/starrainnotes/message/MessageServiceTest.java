/*
 * MessageServiceTest：留言模块的测试文件。
 * 业务范围：留言提交、审核展示和后台管理。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：AccountReferenceApi、CurrentActorApi、ApiException、MessageSubmitDTO、MessageActionEntity、MessageEntity、MessageActionMapper。
 * 阅读入口：setUp（初始化当前测试场景的被测对象与模拟依赖）；publicProjectionNeverExposesPrivateFields（公开ProjectionNeverExposesPrivate字段集合处理入口）；onlyOnePendingDecisionCanCreateAnAction（only单次待处理决定Can创建An动作处理入口）；loggedInSubmissionUsesAccountDisplayName（logged内部SubmissionUses账号显示名称处理入口）。
 */

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

/**
 * 留言业务服务测试，属于留言模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {
    /**
     * messages。
     * 协作类型：<code>MessageMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock private MessageMapper messages;
    /**
     * actions。
     * 协作类型：<code>MessageActionMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock private MessageActionMapper actions;
    /**
     * 当前操作对应的身份对象。
     * 协作类型：<code>CurrentActorApi</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock private CurrentActorApi actor;
    /**
     * 账号集合。
     * 协作类型：<code>AccountReferenceApi</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock private AccountReferenceApi accounts;
    /**
     * 业务服务。
     */
    private MessageServiceImpl service;

    /**
     * 初始化当前测试场景的被测对象与模拟依赖。
     */
    @BeforeEach
    void setUp() {
        service = new MessageServiceImpl(messages, actions, actor, accounts, new MessageSubmissionLimiter());
    }

    /**
     * 公开ProjectionNeverExposesPrivate字段集合处理入口。
     * 主要协作调用：<code>messages.countByStatus</code>、<code>messages.pageByStatus</code>、<code>service.publicMessages</code>。
     */
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

    /**
     * only单次待处理决定Can创建An动作处理入口。
     * 主要协作调用：<code>actor.current</code>、<code>messages.decide</code>、<code>messages.selectById</code>、<code>service.approve</code>。
     */
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

    /**
     * logged内部SubmissionUses账号显示名称处理入口。
     * 主要协作调用：<code>actor.currentOptional</code>、<code>accounts.displayName</code>、<code>service.submit</code>。
     */
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

    /**
     * submission是否RateLimited处理入口。
     * 关键处理步骤调用 <code>limiter.check</code>、<code>error.getCode</code>、<code>error.getStatus</code>。
     */
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
