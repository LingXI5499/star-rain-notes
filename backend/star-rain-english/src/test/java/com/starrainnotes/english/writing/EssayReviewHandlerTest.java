package com.starrainnotes.english.writing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.knowledge.dto.RevisionDto;
import com.starrainnotes.english.knowledge.service.DocumentMetadataService;
import com.starrainnotes.english.writing.article.dto.WritingArticleDto.Article;
import com.starrainnotes.english.writing.article.mapper.WritingArticleMapper;
import com.starrainnotes.english.writing.article.service.impl.EssayReviewHandler;
import com.starrainnotes.review.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class EssayReviewHandlerTest {
    WritingArticleMapper mapper=mock(WritingArticleMapper.class);
    DocumentMetadataService metadata=mock(DocumentMetadataService.class);
    ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
    EssayReviewHandler handler=new EssayReviewHandler(mapper,metadata,json,events);
    ReviewTargetRef ref=ReviewTargetRef.builder().targetModule("ENGLISH").targetType("ESSAY").targetId(9L).revisionRef("1").build();
    ReviewDecisionContext context=ReviewDecisionContext.builder().target(ref).applicantAccountId(3L).build();

    void prepare(String state,long version) {
        Article frozen=new Article();frozen.setId(9L);frozen.setOwnerAccountId(3L);frozen.setRowVersion(4L);frozen.setTitle("雨后");frozen.setBodyMarkdown("After the rain.");
        RevisionDto.Revision revision=new RevisionDto.Revision();revision.setSnapshot(json.valueToTree(frozen));
        when(metadata.revision("WRITING",9L,1L)).thenReturn(revision);
        Article current=new Article();current.setId(9L);current.setOwnerAccountId(3L);current.setRowVersion(version);current.setState(state);
        when(mapper.lockAny(9L)).thenReturn(current);
    }

    @Test void approvalPublishesOnlyFrozenVersionIncludingJsonRoundTrip() {
        prepare("PENDING",5L);when(mapper.status(9L,3L,5L,"COMPLETED","PUBLIC")).thenReturn(1);
        handler.onApproved(context);
        verify(mapper).status(9L,3L,5L,"COMPLETED","PUBLIC");verify(events).publishEvent(any(Object.class));
    }
    @Test void rejectionReturnsAnEditablePrivateArticle() {
        prepare("PENDING",5L);when(mapper.status(9L,3L,5L,"REJECTED","PRIVATE")).thenReturn(1);
        handler.onRejected(context);verify(mapper).status(9L,3L,5L,"REJECTED","PRIVATE");
    }
    @Test void cancellationReturnsDraft() {
        prepare("PENDING",5L);when(mapper.status(9L,3L,5L,"DRAFT","PRIVATE")).thenReturn(1);
        handler.onCanceled(context);verify(mapper).status(9L,3L,5L,"DRAFT","PRIVATE");
    }
    @Test void oldDecisionCannotPublishANewerSubmission() {
        prepare("PENDING",8L);assertThrows(ApiException.class,()->handler.onApproved(context));
        verify(mapper,never()).status(anyLong(),anyLong(),anyLong(),anyString(),anyString());
    }
    @Test void repeatDecisionDoesNotRepublish() {
        prepare("COMPLETED",6L);handler.onApproved(context);verify(mapper,never()).status(anyLong(),anyLong(),anyLong(),anyString(),anyString());
    }
    @Test void wrongApplicantCannotApplyApproval() {
        prepare("PENDING",5L);context.setApplicantAccountId(88L);assertThrows(ApiException.class,()->handler.onApproved(context));
    }
    @Test void reviewViewContainsTheFrozenBilingualDocument() {
        prepare("PENDING",5L);var view=(EssayReviewHandler.View)handler.loadReviewView(ref);
        assertEquals("雨后",view.title());assertEquals("After the rain.",view.contentSnapshot().getBodyMarkdown());assertEquals("PENDING",view.executionStatus());
    }
}
