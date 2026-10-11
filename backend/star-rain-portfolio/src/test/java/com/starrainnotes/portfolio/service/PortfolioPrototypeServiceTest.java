package com.starrainnotes.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.StaticPrototypeApi;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.utils.MediaReferenceCommands;
import com.starrainnotes.portfolio.controller.PortfolioPrototypeController;
import com.starrainnotes.portfolio.dto.WorkPrototypeDTO;
import com.starrainnotes.portfolio.entity.WorkEntity;
import com.starrainnotes.portfolio.event.WorkEventPublisher;
import com.starrainnotes.portfolio.mapper.WorkMapper;
import com.starrainnotes.portfolio.service.impl.PortfolioPrototypeServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PortfolioPrototypeServiceTest {
    @Test
    void bindingUsesTheRealMediaReferenceContractAndClearingDetachesIt() {
        WorkMapper works = mock(WorkMapper.class);
        StaticPrototypeApi prototypes = mock(StaticPrototypeApi.class);
        MediaReferenceApi references = mock(MediaReferenceApi.class);
        CurrentActorApi actor = mock(CurrentActorApi.class);
        WorkEventPublisher events = mock(WorkEventPublisher.class);
        WorkEntity work = new WorkEntity(); work.setId(1L); work.setWorkType("SOFTWARE"); work.setStatus("DRAFT");
        when(works.byIdForUpdate(1L)).thenReturn(work);
        when(actor.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(1L).build());
        var service = new PortfolioPrototypeServiceImpl(works, prototypes, references, actor, events);
        WorkPrototypeDTO request = new WorkPrototypeDTO(); request.setMediaAssetId(20L); request.setEntryPath("index.html");
        service.bind(1L, request);
        verify(prototypes).validate(20L, "index.html");
        var command = ArgumentCaptor.forClass(MediaReferenceCommand.class); verify(references).attach(command.capture());
        assertThat(MediaReferenceCommands.normalize(command.getValue()).getUsageCode()).isEqualTo("portfolio.prototype");
        assertThat(work.getPrototypeAssetId()).isEqualTo(20L);
        service.bind(1L, new WorkPrototypeDTO());
        assertThat(work.getPrototypeAssetId()).isNull();
        assertThat(work.getPrototypeEntry()).isNull();
        verify(references, times(2)).detachAll("PORTFOLIO", "WORK_PROTOTYPE", 1L);
    }

    @Test
    void prototypeResponseAllowsItsOwnFrameAndKeepsScriptIsolation() {
        PortfolioPrototypeService service = mock(PortfolioPrototypeService.class);
        when(service.read("demo", "index.html")).thenReturn(new byte[]{1});
        when(service.contentType("index.html")).thenReturn("text/html");
        var response = new PortfolioPrototypeController(service).read("demo", "/index.html");
        assertThat(response.getHeaders().getFirst("X-Frame-Options")).isEqualTo("SAMEORIGIN");
        assertThat(response.getHeaders().getFirst("Content-Security-Policy"))
                .contains("sandbox allow-scripts", "connect-src 'none'", "frame-src 'none'")
                .doesNotContain("allow-same-origin");
    }
}
