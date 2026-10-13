/*
 * TutorialOperationServiceTest：教程与学习模块的测试文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：CurrentActorApi、ReviewSubmissionApi、ReviewSubmissionResult、TutorialCreateDTO、ChapterUpdateDTO、TutorialOperationEntity、TutorialStateException。
 * 阅读入口：setup（setup处理入口）；administratorCreationOnlyFreezesAProposalUntilApproval（administratorCreationOnlyFreezesA提案UntilApproval处理入口）；superAdministratorExecutesWithoutCreatingAReview（superAdministratorExecutes不含CreatingA审核处理入口）；rejectionAndCancellationReleaseOnlyTheProposal（rejection及CancellationReleaseOnlyThe提案处理入口）。
 */

package com.starrainnotes.tutorial.content.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.review.api.ReviewSubmissionApi;
import com.starrainnotes.review.api.dto.ReviewSubmissionResult;
import com.starrainnotes.tutorial.content.dto.TutorialCreateDTO;
import com.starrainnotes.tutorial.content.dto.ChapterUpdateDTO;
import com.starrainnotes.tutorial.content.entity.TutorialOperationEntity;
import com.starrainnotes.tutorial.content.exception.TutorialStateException;
import com.starrainnotes.tutorial.content.mapper.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 教程操作业务服务测试，属于教程与学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class TutorialOperationServiceTest {
    /** Recovery rechecks a frozen failed operation and an applied retry has no second side effect. */
    @Test void failedOperationRecoversOnce() {
        service.request("createCategory", new Object[]{"Java"});
        var capture = ArgumentCaptor.forClass(TutorialOperationEntity.class);
        verify(operations).insert(capture.capture());
        var frozen = capture.getValue(); frozen.setStatus("FAILED");
        when(operations.byIdForUpdate(8L)).thenReturn(frozen);
        when(operations.finish(8L, "APPLIED")).thenAnswer(call -> { frozen.setStatus("APPLIED"); return 1; });
        service.recoverApproved(8L); service.recoverApproved(8L);
        verify(content, times(1)).createCategory("Java");
    }
    /** Changed content is rejected during recovery, just as during the original callback. */
    @Test void recoveryRefusesChangedBaseline() {
        service.request("createCategory", new Object[]{"Java"});
        var capture = ArgumentCaptor.forClass(TutorialOperationEntity.class); verify(operations).insert(capture.capture());
        var frozen = capture.getValue(); frozen.setStatus("FAILED");
        frozen.setBaselineJson("{\"categories\":[\"newer content\"],\"tutorials\":[]}");
        when(operations.byIdForUpdate(8L)).thenReturn(frozen);
        assertThatThrownBy(() -> service.recoverApproved(8L)).isInstanceOf(TutorialStateException.class);
        verify(content, never()).createCategory(any());
    }
    /** Rejection and cancellation cannot be revived through the recovery entry point. */
    @Test void terminalRejectedOperationsCannotRecover() {
        for (String status : List.of("REJECTED", "CANCELED")) {
            var row = new TutorialOperationEntity(); row.setStatus(status);
            when(operations.byIdForUpdate(8L)).thenReturn(row);
            assertThatThrownBy(() -> service.recoverApproved(8L)).isInstanceOf(TutorialStateException.class);
        }
        verifyNoInteractions(content, publication, exercises);
    }
    /**
     * 内容。
     * 协作类型：<code>TutorialContentService</code>。
     */
    final TutorialContentService content = mock(TutorialContentService.class);
    /**
     * exercises。
     * 协作类型：<code>TutorialExerciseService</code>。
     */
    final TutorialExerciseService exercises = mock(TutorialExerciseService.class);
    /**
     * 发布。
     * 协作类型：<code>TutorialPublicationService</code>。
     */
    final TutorialPublicationService publication = mock(TutorialPublicationService.class);
    /**
     * operations。
     * 协作类型：<code>TutorialOperationMapper</code>。
     */
    final TutorialOperationMapper operations = mock(TutorialOperationMapper.class);
    /**
     * tutorials。
     * 协作类型：<code>TutorialMapper</code>。
     */
    final TutorialMapper tutorials = mock(TutorialMapper.class);
    /**
     * actors。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    final CurrentActorApi actors = mock(CurrentActorApi.class);
    /**
     * reviews。
     * 协作类型：<code>ReviewSubmissionApi</code>。
     */
    final ReviewSubmissionApi reviews = mock(ReviewSubmissionApi.class);
    /**
     * 业务服务。
     * 协作类型：<code>TutorialOperationService</code>。
     */
    TutorialOperationService service;
    /**
     * setup处理入口。
     * 主要协作调用：<code>actors.current</code>、<code>reviews.submit</code>、<code>operations.insert</code>。
     */
    @BeforeEach void setup() {
        service = new TutorialOperationService(content, exercises, publication, operations, tutorials,
                mock(TutorialGroupMapper.class), mock(TutorialChapterMapper.class), mock(TutorialKnowledgeCardMapper.class),
                mock(TutorialQuestionMapper.class), actors, reviews, new ObjectMapper().findAndRegisterModules());
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(7L).roles(Set.of("ADMIN")).build());
        when(reviews.submit(any())).thenReturn(ReviewSubmissionResult.builder().reviewRequestId(9L).status("PENDING").build());
        when(operations.insert(any())).thenAnswer(call -> { ((TutorialOperationEntity) call.getArgument(0)).setId(8L); return 1; });
    }
    /**
     * administratorCreationOnlyFreezesA提案UntilApproval处理入口。
     * 主要协作调用：<code>service.request</code>、<code>operations.byIdForUpdate</code>、<code>service.approve</code>。
     */
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
    /**
     * superAdministratorExecutes不含CreatingA审核处理入口。
     * 主要协作调用：<code>actors.current</code>、<code>service.request</code>。
     */
    @Test void superAdministratorExecutesWithoutCreatingAReview() {
        when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(1L).roles(Set.of("SUPER_ADMIN")).build());
        service.request("createCategory", new Object[]{"Java"});
        verify(content).createCategory("Java");
        verifyNoInteractions(reviews);
        verify(operations, never()).insert(any());
    }
    /**
     * rejection及CancellationReleaseOnlyThe提案处理入口。
     * 主要协作调用：<code>service.finish</code>。
     */
    @Test void rejectionAndCancellationReleaseOnlyTheProposal() {
        service.finish(8L, "REJECTED"); service.finish(9L, "CANCELED");
        verify(operations).finish(8L, "REJECTED"); verify(operations).finish(9L, "CANCELED");
        verifyNoInteractions(content, publication, exercises);
    }
    /**
     * stale已批准提案CannotOverwriteChanged内容处理入口。
     * 主要协作调用：<code>service.request</code>、<code>operations.byIdForUpdate</code>、<code>service.approve</code>。
     */
    @Test void staleApprovedProposalCannotOverwriteChangedContent() {
        service.request("createCategory", new Object[]{"Java"});
        var capture = ArgumentCaptor.forClass(TutorialOperationEntity.class); verify(operations).insert(capture.capture());
        var frozen = capture.getValue(); frozen.setBaselineJson("{\"categories\":[],\"tutorials\":[{\"id\":\"changed\"}]}");
        when(operations.byIdForUpdate(8L)).thenReturn(frozen);
        assertThatThrownBy(() -> service.approve(8L)).isInstanceOf(TutorialStateException.class);
        verify(content, never()).createCategory(any());
    }
    /**
     * 待处理操作BlocksBothDuplicate及SuperAdministratorEdits处理入口。
     * 主要协作调用：<code>operations.pending</code>、<code>service.request</code>。
     */
    @Test void pendingOperationBlocksBothDuplicateAndSuperAdministratorEdits() {
        when(operations.pending("catalog")).thenReturn(new TutorialOperationEntity());
        assertThatThrownBy(() -> service.request("createCategory", new Object[]{"Java"})).isInstanceOf(TutorialStateException.class);
        verify(content, never()).createCategory(any());
        verifyNoInteractions(reviews);
    }
    /**
     * approval重试Does不ExecuteTwice处理入口。
     * 主要协作调用：<code>operations.byIdForUpdate</code>、<code>service.approve</code>。
     */
    @Test void approvalRetryDoesNotExecuteTwice() {
        var applied = new TutorialOperationEntity(); applied.setStatus("APPLIED");
        when(operations.byIdForUpdate(8L)).thenReturn(applied);
        service.approve(8L);
        verifyNoInteractions(content, publication, exercises);
    }

    /**
     * 已删除章节Turns版本校验IntoBusiness错误Instead所属空值Pointer处理入口。
     * 主要协作调用：<code>service.request</code>。
     */
    @Test void deletedChapterTurnsVersionValidationIntoBusinessErrorInsteadOfNullPointer() {
        var dto = new ChapterUpdateDTO(); dto.setRowVersion(2L); dto.setTitle("Local input");
        assertThatThrownBy(() -> service.request("updateChapter", new Object[]{999L, dto}))
                .isInstanceOf(TutorialStateException.class).hasMessageContaining("不存在");
        verifyNoInteractions(reviews);
    }
}
