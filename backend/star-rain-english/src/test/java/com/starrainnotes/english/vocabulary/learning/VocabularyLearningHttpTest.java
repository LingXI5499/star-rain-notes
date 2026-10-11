/*
 * VocabularyLearningHttpTest：英语学习模块的测试文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：CurrentActorApi、ApiException、GlobalApiExceptionHandler、Plan。
 * 阅读入口：setup（setup处理入口）；clientAccountIdCannotChooseAnotherAccount（客户端账号标识CannotChooseAnother账号处理入口）；unauthenticatedRequestDoesNotReadLearningData（unauthenticated请求Does不读取学习Data处理入口）；previewAndMixedDirectionsCannotBecomeRatings（预览及MixedDirectionsCannotBecomeRatings）。
 */

package com.starrainnotes.english.vocabulary.learning;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.common.handler.GlobalApiExceptionHandler;
import com.starrainnotes.english.vocabulary.controller.*;
import com.starrainnotes.english.vocabulary.learning.VocabularyLearningModels.Plan;
import com.starrainnotes.english.vocabulary.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Exercises request binding, actor isolation and the HTTP error boundary without side effects. */
class VocabularyLearningHttpTest {
    /**
     * 基础。
     */
    private static final String BASE = "/api/account/english/vocabulary";
    /**
     * actors。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private CurrentActorApi actors;
    /**
     * 学习。
     * 协作类型：<code>VocabularyLearningService</code>。
     */
    private VocabularyLearningService learning;
    /**
     * commands。
     * 协作类型：<code>VocabularyStudyCommandService</code>。
     */
    private VocabularyStudyCommandService commands;
    /**
     * mvc。
     */
    private MockMvc mvc;
    /**
     * setup处理入口。
     * 主要协作调用：<code>actors.current</code>。
     */
    @BeforeEach void setup() {
        actors = mock(CurrentActorApi.class); learning = mock(VocabularyLearningService.class);
        commands = mock(VocabularyStudyCommandService.class);
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(17L).build());
        mvc = MockMvcBuilders.standaloneSetup(new VocabularyLearningController(actors, learning),
                new VocabularyStudyAccountController(mock(VocabularyStudyQueryService.class), commands,
                        mock(VocabularyProgressImportService.class), actors, learning))
                .setControllerAdvice(new GlobalApiExceptionHandler()).build();
    }
    /**
     * 客户端账号标识CannotChooseAnother账号处理入口。
     * 主要协作调用：<code>learning.plan</code>、<code>mvc.perform</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void clientAccountIdCannotChooseAnotherAccount() throws Exception {
        when(learning.plan(17)).thenReturn(new Plan());
        mvc.perform(get(BASE + "/plan").param("accountId", "999")).andExpect(status().isOk());
        verify(learning).plan(17); verify(learning, never()).plan(999);
    }
    /**
     * unauthenticated请求Does不读取学习Data处理入口。
     * 主要协作调用：<code>actors.current</code>、<code>mvc.perform</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void unauthenticatedRequestDoesNotReadLearningData() throws Exception {
        when(actors.current()).thenThrow(new AuthenticationCredentialsNotFoundException("missing"));
        mvc.perform(get(BASE + "/plan")).andExpect(status().isUnauthorized()); verifyNoInteractions(learning);
    }
    /**
     * 预览及MixedDirectionsCannotBecomeRatings。
     * 主要协作调用：<code>mvc.perform</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void previewAndMixedDirectionsCannotBecomeRatings() throws Exception {
        for (String direction : new String[] {"BILINGUAL_PREVIEW", "MIXED"}) {
            mvc.perform(post(BASE + "/words/1/reviews").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reviewSessionId\":\"11111111-1111-4111-8111-111111111111\",\"direction\":\"" + direction
                            + "\",\"rating\":\"KNOW\",\"source\":\"FREE\"}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(learning);
    }
    /**
     * stale修订Remains冲突时间HTTP边界处理入口。
     * 主要协作调用：<code>learning.confirm</code>、<code>mvc.perform</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void staleRevisionRemainsConflictAtHttpBoundary() throws Exception {
        when(learning.confirm(eq(17L), any())).thenThrow(new ApiException("ENGLISH_VOCABULARY_LEARNING_CONFLICT", "refresh", 409));
        mvc.perform(put(BASE + "/plan").contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedRevision\":9,\"wordIds\":[1],\"previewFingerprint\":\"stale\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ENGLISH_VOCABULARY_LEARNING_CONFLICT"));
    }
    /**
     * 空值SelectionListsAreBad请求之前Any写入处理入口。
     * 主要协作调用：<code>mvc.perform</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void nullSelectionListsAreBadRequestBeforeAnyWrite() throws Exception {
        mvc.perform(post(BASE + "/plan/preview").contentType(MediaType.APPLICATION_JSON).content("{\"wordIds\":null}"))
                .andExpect(status().isBadRequest()); verifyNoInteractions(learning);
    }
    /**
     * legacyResetCannot删除Permanent记忆处理入口。
     * 主要协作调用：<code>mvc.perform</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void legacyResetCannotDeletePermanentMemory() throws Exception {
        mvc.perform(delete(BASE + "/words/1/progress")).andExpect(status().isConflict()); verifyNoInteractions(commands);
    }
}
