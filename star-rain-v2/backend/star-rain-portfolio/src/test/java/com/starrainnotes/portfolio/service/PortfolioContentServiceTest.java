package com.starrainnotes.portfolio.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.media.api.*;
import com.starrainnotes.media.api.dto.*;
import com.starrainnotes.portfolio.dto.*;
import com.starrainnotes.portfolio.entity.*;
import com.starrainnotes.portfolio.event.WorkEventPublisher;
import com.starrainnotes.portfolio.exception.*;
import com.starrainnotes.portfolio.mapper.*;
import com.starrainnotes.portfolio.service.impl.PortfolioContentServiceImpl;
import com.starrainnotes.portfolio.validator.WorkSectionRules;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

class PortfolioContentServiceTest {
    private final WorkContentMapper mapper = mock(WorkContentMapper.class);
    private final WorkMapper works = mock(WorkMapper.class);
    private final MediaAssetApi assets = mock(MediaAssetApi.class);
    private final MediaReferenceApi refs = mock(MediaReferenceApi.class);
    private final CurrentActorApi actor = mock(CurrentActorApi.class);
    private final WorkEventPublisher events = mock(WorkEventPublisher.class);
    private final ObjectMapper json = new ObjectMapper();
    private final PortfolioContentService service = new PortfolioContentServiceImpl(mapper, works, assets, refs, actor, events, json);
    private WorkEntity work;
    private final List<WorkSectionEntity> rows = new ArrayList<>();

    @BeforeEach void setup() {
        work = new WorkEntity(); work.setId(1L); work.setStatus("DRAFT"); work.setSlug("test");
        when(works.byIdForUpdate(1L)).thenReturn(work);
        when(actor.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(1L).build());
        when(mapper.sections(1L)).thenAnswer(i -> new ArrayList<>(rows));
        doAnswer(i -> { WorkSectionEntity row = i.getArgument(0); row.setId((long)rows.size() + 1); rows.add(row); return null; }).when(mapper).insertSection(any());
    }
    @Test void aHiddenBlockCannotLeakOrRestoreLegacyContent() {
        service.create(1L, block("MARKDOWN", "private searchable secret", false));
        assertThat(service.sections(1L, true)).isEmpty();
        assertThat(service.searchableText(1L, "old secret")).isEmpty();
        assertThat(service.seoMarkdown(1L, "old secret")).isEmpty();
        assertThat(service.sections(1L, false)).hasSize(1);
    }
    @Test void legacyBodyIsUsedOnlyWhenThereAreNoBlocks() {
        assertThat(service.searchableText(1L, "legacy content")).isEqualTo("legacy content");
        service.create(1L, block("MARKDOWN", "current content", true));
        assertThat(service.searchableText(1L, "legacy content")).contains("current content").doesNotContain("legacy content");
    }
    @Test void mediaAttachmentCreatesTheCrossDomainDeletionGuard() {
        when(assets.get(20L)).thenReturn(MediaAssetSummary.builder().id(20L).mediaType("IMAGE").accessLevel("PUBLIC").status("ACTIVE").build());
        doAnswer(i -> { WorkSectionMediaEntity row = i.getArgument(0); row.setId(50L); return null; }).when(mapper).insertSectionMedia(any());
        WorkSectionDTO dto = block("IMAGE", "", true); dto.setMedia(List.of(SectionMediaDTO.builder().mediaAssetId(20L).build()));
        service.create(1L, dto);
        var command = ArgumentCaptor.forClass(MediaReferenceCommand.class); verify(refs).attach(command.capture());
        assertThat(command.getValue().getSourceType()).isEqualTo("WORK_SECTION_MEDIA");
        assertThat(command.getValue().getSourceId()).isEqualTo(50L);
        assertThat(command.getValue().getMediaAssetId()).isEqualTo(20L);
        assertThat(com.starrainnotes.media.utils.MediaReferenceCommands.normalize(command.getValue()).getUsageCode())
                .isEqualTo("portfolio.section");
    }

    @Test void markdownSelectedImagesUseTheSameArchiveProtectionAndPublicAccessCheck() {
        when(assets.get(20L)).thenReturn(MediaAssetSummary.builder().id(20L).mediaType("IMAGE")
                .contentUrl("/media/20").accessLevel("PUBLIC").status("ACTIVE").build());
        doAnswer(i -> { WorkSectionMediaEntity row = i.getArgument(0); row.setId(50L); return null; }).when(mapper).insertSectionMedia(any());
        WorkSectionDTO dto = block("MARKDOWN", "![image](/media/20)", true);
        dto.setMedia(List.of(SectionMediaDTO.builder().mediaAssetId(20L).build()));
        service.create(1L, dto);
        var command = ArgumentCaptor.forClass(MediaReferenceCommand.class); verify(refs).attach(command.capture());
        assertThat(com.starrainnotes.media.utils.MediaReferenceCommands.normalize(command.getValue()).getUsageCode())
                .isEqualTo("portfolio.section");
        work.setStatus("PUBLISHED");
        when(assets.get(20L)).thenReturn(MediaAssetSummary.builder().id(20L).mediaType("IMAGE")
                .accessLevel("PROTECTED").status("ACTIVE").build());
        assertThatThrownBy(() -> service.create(1L, dto)).isInstanceOf(WorkInvalidException.class);
    }
    @Test void imageBlockRejectsAudioAndPublishedBlockRejectsPrivateMedia() {
        when(assets.get(20L)).thenReturn(MediaAssetSummary.builder().id(20L).mediaType("AUDIO").accessLevel("PUBLIC").status("ACTIVE").build());
        WorkSectionDTO dto = block("IMAGE", "", true); dto.setMedia(List.of(SectionMediaDTO.builder().mediaAssetId(20L).build()));
        assertThatThrownBy(() -> service.create(1L, dto)).isInstanceOf(WorkInvalidException.class);
        work.setStatus("PUBLISHED");
        when(assets.get(20L)).thenReturn(MediaAssetSummary.builder().id(20L).mediaType("IMAGE").accessLevel("PRIVATE").status("ACTIVE").build());
        assertThatThrownBy(() -> service.create(1L, dto)).isInstanceOf(WorkInvalidException.class);
        verify(mapper, never()).insertSection(any());
    }
    @Test void sectionOwnershipAndCompleteOrderAreEnforced() {
        var other = WorkSectionEntity.builder().id(9L).workId(2L).build(); when(mapper.section(9L)).thenReturn(other);
        assertThatThrownBy(() -> service.update(1L, 9L, block("MARKDOWN", "x", true))).isInstanceOf(WorkNotFoundException.class);
        service.create(1L, block("MARKDOWN", "one", true)); service.create(1L, block("MARKDOWN", "two", true));
        assertThatThrownBy(() -> service.reorder(1L, List.of(1L,1L))).isInstanceOf(WorkInvalidException.class);
        assertThatThrownBy(() -> service.reorder(1L, List.of(1L,9L))).isInstanceOf(WorkInvalidException.class);
        service.reorder(1L, List.of(2L,1L)); verify(mapper).orderSection(2L,10); verify(mapper).orderSection(1L,20);
    }
    @Test void templateCopyIsAnIndependentSnapshotAndBlankTemplateCreatesNoRows() {
        var template = WorkTemplateEntity.builder().id(3L).sectionsJson("[{\"sectionType\":\"MARKDOWN\",\"title\":\"Background\",\"content\":\"\",\"data\":{},\"visible\":true}]").build();
        when(mapper.template(3L)).thenReturn(template); service.copyTemplate(1L,3L);
        template.setSectionsJson("[]"); assertThat(rows.getFirst().getTitle()).isEqualTo("Background");
        when(mapper.template(4L)).thenReturn(WorkTemplateEntity.builder().id(4L).sectionsJson("[]").build());
        service.copyTemplate(1L,4L); assertThat(rows).hasSize(1);
    }
    @Test void publicationRequiresNonemptyVisibleBlocks() {
        service.create(1L, block("MARKDOWN", "", false));
        assertThatThrownBy(() -> service.validatePublication(1L)).isInstanceOf(WorkInvalidException.class);
        rows.getFirst().setVisible(true);
        assertThatThrownBy(() -> service.validatePublication(1L)).isInstanceOf(WorkInvalidException.class);
    }
    @Test void invalidSchemaAndUnsafeLinksAreRejected() throws Exception {
        var dto = block("LINKS", "", true); dto.setData(json.readTree("{\"items\":[{\"url\":\"javascript:alert(1)\",\"label\":\"X\"}]}"));
        assertThatThrownBy(() -> WorkSectionRules.validate(dto)).isInstanceOf(WorkInvalidException.class);
        dto.setSectionType("MARKDOWN"); dto.setData(json.readTree("{\"html\":\"<script>\"}"));
        assertThatThrownBy(() -> WorkSectionRules.validate(dto)).isInstanceOf(WorkInvalidException.class);
        assertThatThrownBy(() -> WorkSectionRules.validate(block("UNKNOWN", "x", true))).isInstanceOf(WorkInvalidException.class);
    }
    @Test void featuredCapAndTagOwnershipAreEnforced() {
        when(mapper.featuredCount(1L)).thenReturn(3L);
        assertThatThrownBy(() -> service.validateFeatured(1L,true)).isInstanceOf(WorkInvalidException.class);
        verify(mapper).lockFeatured();
        when(mapper.tags()).thenReturn(List.of(WorkTaxonomyEntity.builder().id(10L).build()));
        assertThatThrownBy(() -> service.setTags(1L,List.of(20L))).isInstanceOf(WorkInvalidException.class);
        assertThatThrownBy(() -> service.setTags(1L,List.of(10L,10L))).isInstanceOf(WorkInvalidException.class);
        service.setTags(1L,List.of(10L)); verify(mapper).addTag(1L,10L);
    }
    private WorkSectionDTO block(String type, String text, boolean visible) {
        return WorkSectionDTO.builder().sectionType(type).title("A block").content(text).visible(visible).data(json.createObjectNode()).build();
    }
}
