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
    private static final String BASE = "/api/account/english/vocabulary";
    private CurrentActorApi actors;
    private VocabularyLearningService learning;
    private VocabularyStudyCommandService commands;
    private MockMvc mvc;
    @BeforeEach void setup() {
        actors = mock(CurrentActorApi.class); learning = mock(VocabularyLearningService.class);
        commands = mock(VocabularyStudyCommandService.class);
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(17L).build());
        mvc = MockMvcBuilders.standaloneSetup(new VocabularyLearningController(actors, learning),
                new VocabularyStudyAccountController(mock(VocabularyStudyQueryService.class), commands,
                        mock(VocabularyProgressImportService.class), actors, learning))
                .setControllerAdvice(new GlobalApiExceptionHandler()).build();
    }
    @Test void clientAccountIdCannotChooseAnotherAccount() throws Exception {
        when(learning.plan(17)).thenReturn(new Plan());
        mvc.perform(get(BASE + "/plan").param("accountId", "999")).andExpect(status().isOk());
        verify(learning).plan(17); verify(learning, never()).plan(999);
    }
    @Test void unauthenticatedRequestDoesNotReadLearningData() throws Exception {
        when(actors.current()).thenThrow(new AuthenticationCredentialsNotFoundException("missing"));
        mvc.perform(get(BASE + "/plan")).andExpect(status().isUnauthorized()); verifyNoInteractions(learning);
    }
    @Test void previewAndMixedDirectionsCannotBecomeRatings() throws Exception {
        for (String direction : new String[] {"BILINGUAL_PREVIEW", "MIXED"}) {
            mvc.perform(post(BASE + "/words/1/reviews").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reviewSessionId\":\"11111111-1111-4111-8111-111111111111\",\"direction\":\"" + direction
                            + "\",\"rating\":\"KNOW\",\"source\":\"FREE\"}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(learning);
    }
    @Test void staleRevisionRemainsConflictAtHttpBoundary() throws Exception {
        when(learning.confirm(eq(17L), any())).thenThrow(new ApiException("ENGLISH_VOCABULARY_LEARNING_CONFLICT", "refresh", 409));
        mvc.perform(put(BASE + "/plan").contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedRevision\":9,\"wordIds\":[1],\"previewFingerprint\":\"stale\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ENGLISH_VOCABULARY_LEARNING_CONFLICT"));
    }
    @Test void nullSelectionListsAreBadRequestBeforeAnyWrite() throws Exception {
        mvc.perform(post(BASE + "/plan/preview").contentType(MediaType.APPLICATION_JSON).content("{\"wordIds\":null}"))
                .andExpect(status().isBadRequest()); verifyNoInteractions(learning);
    }
    @Test void legacyResetCannotDeletePermanentMemory() throws Exception {
        mvc.perform(delete(BASE + "/words/1/progress")).andExpect(status().isConflict()); verifyNoInteractions(commands);
    }
}
