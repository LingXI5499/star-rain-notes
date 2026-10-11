package com.starrainnotes.portfolio.service.impl;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.media.api.StaticPrototypeApi;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.portfolio.dto.WorkPrototypeDTO;
import com.starrainnotes.portfolio.entity.WorkEntity;
import com.starrainnotes.portfolio.exception.WorkInvalidException;
import com.starrainnotes.portfolio.exception.WorkNotFoundException;
import com.starrainnotes.portfolio.mapper.WorkMapper;
import com.starrainnotes.portfolio.event.WorkEventPublisher;
import com.starrainnotes.portfolio.api.event.WorkPublicationChangedEvent;
import com.starrainnotes.portfolio.service.PortfolioPrototypeService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortfolioPrototypeServiceImpl implements PortfolioPrototypeService {
    private final WorkMapper works;
    private final StaticPrototypeApi prototypes;
    private final MediaReferenceApi references;
    private final CurrentActorApi actor;
    private final WorkEventPublisher events;
    @Override @Transactional public void bind(Long workId, WorkPrototypeDTO request) {
        WorkEntity work = works.byIdForUpdate(workId);
        if (work == null) { throw new WorkNotFoundException(); }
        if (request.getMediaAssetId() != null) {
            if (!"SOFTWARE".equals(work.getWorkType())) { throw new WorkInvalidException("静态原型用于软件作品"); }
            prototypes.validate(request.getMediaAssetId(), request.getEntryPath());
        }
        references.detachAll("PORTFOLIO", "WORK_PROTOTYPE", workId);
        if (request.getMediaAssetId() != null) {
            references.attach(MediaReferenceCommand.builder().mediaAssetId(request.getMediaAssetId())
                    .sourceModule("PORTFOLIO").sourceType("WORK_PROTOTYPE").sourceId(workId).usageCode(com.starrainnotes.media.api.constant.MediaUsageCodes.PORTFOLIO_PROTOTYPE).build());
        }
        work.setPrototypeAssetId(request.getMediaAssetId());
        work.setPrototypeEntry(request.getMediaAssetId() == null ? null : request.getEntryPath());
        work.setUpdatedByAccountId(actor.current().getAccountId());
        work.setUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        works.updatePrototype(work);
        if ("PUBLISHED".equals(work.getStatus())) { events.afterCommit(WorkPublicationChangedEvent.builder().workId(workId).slug(work.getSlug()).published(true).build()); }
    }
    @Override @Transactional(readOnly = true) public byte[] read(String slug, String path) {
        WorkEntity work = works.selectBySlugAndStatus(slug, "PUBLISHED");
        if (work == null || work.getPrototypeAssetId() == null) { throw new WorkNotFoundException(); }
        return prototypes.read(work.getPrototypeAssetId(), path);
    }
    @Override public String contentType(String path) { return prototypes.contentType(path); }
}
