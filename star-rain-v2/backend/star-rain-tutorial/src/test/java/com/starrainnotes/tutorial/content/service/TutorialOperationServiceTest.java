package com.starrainnotes.tutorial.content.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.review.api.ReviewSubmissionApi;
import com.starrainnotes.review.api.dto.ReviewSubmissionResult;
import com.starrainnotes.tutorial.content.dto.TutorialCreateDTO;
import com.starrainnotes.tutorial.content.entity.TutorialOperationEntity;
import com.starrainnotes.tutorial.content.exception.TutorialStateException;
import com.starrainnotes.tutorial.content.mapper.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TutorialOperationServiceTest {
    final TutorialContentService content = mock(TutorialContentService.class);
    final TutorialExerciseService exercises = mock(TutorialExerciseService.class);
    final TutorialPublicationService publication = mock(TutorialPublicationService.class);
    final TutorialOperationMapper operations = mock(TutorialOperationMapper.class);
    final TutorialMapper tutorials = mock(TutorialMapper.class);
    final CurrentActorApi actors = mock(CurrentActorApi.class);
    final ReviewSubmissionApi reviews = mock(ReviewSubmissionApi.class);
    TutorialOperationService service;
    @BeforeEach void setup() {
        service = new TutorialOperationService(content, exercises, publication, operations, tutorials,
                mock(TutorialGroupMapper.class), mock(TutorialChapterMapper.class), mock(TutorialKnowledgeCardMapper.class),
                mock(TutorialQuestionMapper.class), actors, reviews, new ObjectMapper().findAndRegisterModules());
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(7L).roles(Set.of("ADMIN")).build());
        when(reviews.submit(any())).thenReturn(ReviewSubmissionResult.builder().reviewRequestId(9L).status("PENDING").build());
        when(operations.insert(any())).thenAnswer(call -> { ((TutorialOperationEntity) call.getArgument(0)).setId(8L); return 1; });
    }
    @Test void administratorCreationOnlyFreezesAProposalUntilApproval() {
        var dto = new TutorialCreateDTO(); dto.setCategoryId(2L); dto.setTitle("Java"); dto.setSummary("Summary");
        Object response = service.request("createTutorial", new Object[]{dto});
        assertThat(((Map<?, ?>) response).get("reviewPending")).isEqualTo(true);
        verify(content, never()).createTutorial(any());
        verifyNoInteractions(publication);
        var capture = ArgumentCaptor.forClass(TutorialOperationEntity.class);
        verify(operations).insert(capture.capture());
        var frozen = capture.getValue();
        when(operations.byIdForUpdate(8L)).thenReturn(frozen);
        dto.setTitle("Changed locally after submission");
        service.approve(8L);
        verify(content).createTutorial(argThat(request -> "Java".equals(request.getTitle())));
        verify(operations).finish(8L, "APPLIED");
    }
    @Test void superAdministratorExecutesWithoutCreatingAReview() {
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(1L).roles(Set.of("SUPER_ADMIN")).build());
        service.request("createCategory", new Object[]{"Java"});
        verify(content).createCategory("Java");
        verifyNoInteractions(reviews);
        verify(operations, never()).insert(any());
    }
    @Test void rejectionAndCancellationReleaseOnlyTheProposal() {
        service.finish(8L, "REJECTED"); service.finish(9L, "CANCELED");
        verify(operations).finish(8L, "REJECTED"); verify(operations).finish(9L, "CANCELED");
        verifyNoInteractions(content, publication, exercises);
    }
    @Test void staleApprovedProposalCannotOverwriteChangedContent() {
        service.request("createCategory", new Object[]{"Java"});
        var capture = ArgumentCaptor.forClass(TutorialOperationEntity.class); verify(operations).insert(capture.capture());
        var frozen = capture.getValue(); frozen.setBaselineJson("{\"categories\":[],\"tutorials\":[{\"id\":\"changed\"}]}");
        when(operations.byIdForUpdate(8L)).thenReturn(frozen);
        assertThatThrownBy(() -> service.approve(8L)).isInstanceOf(TutorialStateException.class);
        verify(content, never()).createCategory(any());
    }
    @Test void pendingOperationBlocksBothDuplicateAndSuperAdministratorEdits() {
        when(operations.pending("catalog")).thenReturn(new TutorialOperationEntity());
        assertThatThrownBy(() -> service.request("createCategory", new Object[]{"Java"})).isInstanceOf(TutorialStateException.class);
        verify(content, never()).createCategory(any());
        verifyNoInteractions(reviews);
    }
    @Test void approvalRetryDoesNotExecuteTwice() {
        var applied = new TutorialOperationEntity(); applied.setStatus("APPLIED");
        when(operations.byIdForUpdate(8L)).thenReturn(applied);
        service.approve(8L);
        verifyNoInteractions(content, publication, exercises);
    }
}
