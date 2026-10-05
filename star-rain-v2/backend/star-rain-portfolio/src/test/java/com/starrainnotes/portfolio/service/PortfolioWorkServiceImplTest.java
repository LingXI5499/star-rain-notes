package com.starrainnotes.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.media.api.MediaAssetApi;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.portfolio.entity.WorkDetailEntity;
import com.starrainnotes.portfolio.entity.WorkEntity;
import com.starrainnotes.portfolio.entity.WorkMediaEntity;
import com.starrainnotes.portfolio.event.WorkEventPublisher;
import com.starrainnotes.portfolio.exception.WorkNotFoundException;
import com.starrainnotes.portfolio.exception.WorkStateInvalidException;
import com.starrainnotes.portfolio.mapper.WorkDetailMapper;
import com.starrainnotes.portfolio.mapper.WorkLinkMapper;
import com.starrainnotes.portfolio.mapper.WorkMapper;
import com.starrainnotes.portfolio.mapper.WorkMediaMapper;
import com.starrainnotes.portfolio.service.impl.PortfolioWorkServiceImpl;
import com.starrainnotes.portfolio.validator.impl.OtherDetailValidator;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PortfolioWorkServiceImplTest {
    private final WorkMapper works = mock(WorkMapper.class);
    private final WorkDetailMapper details = mock(WorkDetailMapper.class);
    private final WorkMediaMapper media = mock(WorkMediaMapper.class);
    private final WorkLinkMapper links = mock(WorkLinkMapper.class);
    private final MediaAssetApi assets = mock(MediaAssetApi.class);
    private final MediaReferenceApi references = mock(MediaReferenceApi.class);
    private final CurrentActorApi actor = mock(CurrentActorApi.class);
    private final WorkEventPublisher events = mock(WorkEventPublisher.class);
    private final PortfolioWorkServiceImpl service = new PortfolioWorkServiceImpl(
            works, details, media, links, assets, references, actor, new ObjectMapper(),
            List.of(new OtherDetailValidator()), events);

    @BeforeEach
    void actorIsAvailable() {
        when(actor.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(7L).build());
    }

    @Test
    void withdrawnWorkRestoresWithoutChangingFirstPublicationTime() {
        LocalDateTime firstPublished = LocalDateTime.of(2026, 9, 1, 10, 30);
        WorkEntity work = work("WITHDRAWN", firstPublished);
        WorkDetailEntity detail = new WorkDetailEntity();
        detail.setDetailJson("{}");
        when(works.byIdForUpdate(9L)).thenReturn(work);
        when(details.selectByWorkId(any())).thenReturn(detail);
        when(media.listByWorkId(any())).thenReturn(List.of());
        when(links.listByWorkId(any())).thenReturn(List.of());

        assertThat(service.restore(9L).getStatus()).isEqualTo("PUBLISHED");
        assertThat(work.getPublishedAt()).isEqualTo(firstPublished);
        verify(events).afterCommit(any());
    }

    @Test
    void publishingWithoutTypeDetailFailsBeforeStateChange() {
        WorkEntity work = work("DRAFT", null);
        when(works.byIdForUpdate(9L)).thenReturn(work);

        assertThatThrownBy(() -> service.publish(9L)).isInstanceOf(WorkStateInvalidException.class);
        assertThat(work.getStatus()).isEqualTo("DRAFT");
    }

    @Test
    void publicLookupDoesNotRevealUnpublishedWork() {
        assertThatThrownBy(() -> service.publicWork("private-draft"))
                .isInstanceOf(WorkNotFoundException.class);
    }

    @Test
    void removingMediaDetachesItsReference() {
        WorkMediaEntity attached = new WorkMediaEntity();
        attached.setId(3L);
        attached.setWorkId(9L);
        attached.setMediaAssetId(77L);
        attached.setUsageType("COVER");
        when(media.selectById(3L)).thenReturn(attached);
        when(works.byIdForUpdate(9L)).thenReturn(work("DRAFT", null));

        service.removeMedia(3L);

        verify(media).deleteById(3L);
        ArgumentCaptor<MediaReferenceCommand> command = ArgumentCaptor.forClass(MediaReferenceCommand.class);
        verify(references).detach(command.capture());
        assertThat(command.getValue().getMediaAssetId()).isEqualTo(77L);
        assertThat(command.getValue().getSourceId()).isEqualTo(3L);
    }

    @Test
    void publishedWorkMustBeWithdrawnBeforeDeletion() {
        when(works.byIdForUpdate(9L)).thenReturn(work("PUBLISHED", LocalDateTime.now()));
        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(WorkStateInvalidException.class);
    }

    private WorkEntity work(String status, LocalDateTime publishedAt) {
        WorkEntity work = new WorkEntity();
        work.setId(9L);
        work.setSlug("sample-work");
        work.setWorkType("OTHER");
        work.setTitle("Sample");
        work.setSummary("A meaningful case study");
        work.setBodyMarkdown("# Case Study");
        work.setStatus(status);
        work.setPublishedAt(publishedAt);
        return work;
    }
}
