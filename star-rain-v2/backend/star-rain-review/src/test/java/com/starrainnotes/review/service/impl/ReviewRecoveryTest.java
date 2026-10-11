package com.starrainnotes.review.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.starrainnotes.review.api.*;
import com.starrainnotes.review.entity.*;
import com.starrainnotes.review.handler.*;
import com.starrainnotes.review.mapper.*;
import com.starrainnotes.review.exception.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReviewRecoveryTest {
    @Test void onlyApprovedSupportedTargetsRecoverAndEveryAttemptIsAudited() {
        var requests = mock(ReviewRequestMapper.class); var actions = mock(ReviewActionMapper.class);
        var registry = mock(ReviewTargetHandlerRegistry.class); var handler = mock(ReviewTargetHandler.class);
        var service = new ReviewDecisionServiceImpl(requests, actions, registry, new ReviewDecisionCallbackDispatcher());
        var request = new ReviewRequestEntity(); request.setId(1L); request.setTargetModule("TUTORIAL");
        request.setTargetType("OPERATION"); request.setReviewType("tutorial.operation"); request.setTargetId(2L);
        request.setTargetRevisionRef("tutorial-operation:2"); request.setStatus("PENDING");
        when(requests.requestById(1L)).thenReturn(request);
        assertThrows(ReviewAlreadyCompletedException.class, () -> service.recoverApproved(1L, 7L));
        assertThrows(ReviewAccessDeniedException.class, () -> service.recoverApproved(1L, null));
        verifyNoInteractions(actions);
        request.setStatus("APPROVED"); when(registry.require("TUTORIAL", "OPERATION", "tutorial.operation")).thenReturn(handler);
        assertThrows(ReviewAccessDeniedException.class, () -> service.recoverApproved(1L, 7L));
        when(handler.supportsApprovedRecovery()).thenReturn(true);
        service.recoverApproved(1L, 7L);
        var capture = ArgumentCaptor.forClass(ReviewActionEntity.class); verify(actions).insertAction(capture.capture());
        assertEquals("RECOVERY_REQUESTED", capture.getValue().getActionType());
        verify(handler).recoverApproved(argThat(context -> "APPROVED".equals(context.getStatus()) && context.getTarget().getTargetId() == 2L));
        verify(requests, never()).approveIfPending(any(), any(), any(), any());
    }
}
