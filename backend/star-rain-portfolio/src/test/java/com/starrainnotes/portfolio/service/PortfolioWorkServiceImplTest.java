/*
 * PortfolioWorkServiceImplTest：作品模块的测试文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：CurrentActorApi、MediaAssetApi、MediaReferenceApi、MediaReferenceCommand、WorkDetailEntity、WorkEntity、WorkMediaEntity。
 * 阅读入口：actorIsAvailable（操作者是否Available处理入口）；patchCanExplicitlyClearOptionalDates（patchCanExplicitlyClearOptionalDates处理入口）；withdrawnWorkRestoresWithoutChangingFirstPublicationTime（已撤回作品Restores不含Changing首次发布时间处理入口）；publishingWithoutTypeDetailIsAllowed（publishing不含类型详情是否Allowed处理入口）。
 */

package com.starrainnotes.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.media.api.MediaAssetApi;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.portfolio.entity.WorkDetailEntity;
import com.starrainnotes.portfolio.entity.WorkEntity;
import com.starrainnotes.portfolio.entity.WorkMediaEntity;
import com.starrainnotes.portfolio.entity.WorkLinkEntity;
import com.starrainnotes.portfolio.dto.WorkMediaDTO;
import com.starrainnotes.portfolio.enumeration.WorkMediaUsage;
import com.starrainnotes.portfolio.exception.WorkInvalidException;
import com.starrainnotes.portfolio.vo.WorkSectionVO;
import com.starrainnotes.media.api.dto.MediaAssetSummary;
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

/**
 * 作品作品业务服务实现测试，属于作品模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class PortfolioWorkServiceImplTest {
    @Test
    void completeMarkdownWorkPublishesWithoutCover() {
        WorkEntity work = work("DRAFT", null);
        when(works.byIdForUpdate(9L)).thenReturn(work);
        when(media.listByWorkId(9L)).thenReturn(List.of());
        var result = service.publish(9L);
        assertThat(result.getStatus()).isEqualTo("PUBLISHED");
        assertThat(result.getCoverUrl()).isNull();
    }

    @Test
    void coverStillRequiresPublicAndUsableMedia() {
        when(works.byIdForUpdate(9L)).thenReturn(work("DRAFT", null));
        when(assets.get(77L)).thenReturn(MediaAssetSummary.builder().id(77L).mediaType("IMAGE")
                .accessLevel("PRIVATE").status("ACTIVE").build());
        assertThatThrownBy(() -> service.publish(9L)).isInstanceOf(WorkStateInvalidException.class)
                .hasMessageContaining("媒体必须可公开访问");
        when(assets.get(77L)).thenReturn(MediaAssetSummary.builder().id(77L).mediaType("IMAGE")
                .accessLevel("PUBLIC").status("DISABLED").build());
        doThrow(new WorkInvalidException("媒体不可用")).when(assets).assertUsable(77L);
        assertThatThrownBy(() -> service.publish(9L)).isInstanceOf(WorkInvalidException.class);
    }

    @Test
    void publishedWorkCanRemoveItsLastCoverAndDetachTheReference() {
        WorkMediaEntity cover = media.listByWorkId(9L).getFirst();
        when(media.selectById(3L)).thenReturn(cover);
        when(works.byIdForUpdate(9L)).thenReturn(work("PUBLISHED", LocalDateTime.now()));
        service.removeMedia(3L);
        verify(media).deleteById(3L);
        verify(references).detach(any(MediaReferenceCommand.class));
        verify(works).touch(any());
        verify(events).publish(any());
    }

    @Test
    void publishedCoverCanBecomeAScreenshotWhileRetainingPublicMediaChecks() {
        WorkEntity work = work("PUBLISHED", LocalDateTime.now());
        WorkMediaEntity cover = media.listByWorkId(9L).getFirst();
        when(media.selectById(3L)).thenReturn(cover);
        when(works.byIdForUpdate(9L)).thenReturn(work);
        when(works.selectById(9L)).thenReturn(work);
        WorkMediaDTO request = WorkMediaDTO.builder().mediaAssetId(77L).usageType(WorkMediaUsage.SCREENSHOT).build();
        assertThat(service.updateMedia(3L, request).getCoverUrl()).isNull();
        assertThat(cover.getUsageType()).isEqualTo("SCREENSHOT");
        verify(assets).assertUsable(77L);
        verify(references).detach(any(MediaReferenceCommand.class));
        verify(references).attach(any(MediaReferenceCommand.class));
    }

    @Test
    void publishedCoverReplacementCannotUsePrivateMedia() {
        WorkEntity work = work("PUBLISHED", LocalDateTime.now());
        WorkMediaEntity cover = media.listByWorkId(9L).getFirst();
        when(media.selectById(3L)).thenReturn(cover);
        when(works.byIdForUpdate(9L)).thenReturn(work);
        when(works.selectById(9L)).thenReturn(work);
        when(assets.get(78L)).thenReturn(MediaAssetSummary.builder().id(78L).mediaType("IMAGE")
                .accessLevel("PRIVATE").status("ACTIVE").build());
        assertThatThrownBy(() -> service.updateMedia(3L, WorkMediaDTO.builder().mediaAssetId(78L)
                .usageType(WorkMediaUsage.COVER).build())).isInstanceOf(WorkInvalidException.class);
        verify(media, never()).updateContent(any());
        verify(references, never()).detach(any());
    }

    @Test
    void missingMarkdownAndBlocksCannotPublishEvenWithoutCover() {
        WorkEntity work = work("DRAFT", null); work.setBodyMarkdown("  ");
        when(works.byIdForUpdate(9L)).thenReturn(work);
        when(media.listByWorkId(9L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.publish(9L)).isInstanceOf(WorkStateInvalidException.class)
                .hasMessageContaining("正文或可见内容区块");
    }

    @Test
    void legacyBlocksRemainThePublishAuthority() {
        WorkEntity work = work("DRAFT", null); work.setBodyMarkdown("");
        when(works.byIdForUpdate(9L)).thenReturn(work);
        when(media.listByWorkId(9L)).thenReturn(List.of());
        when(content.sections(9L, false)).thenReturn(List.of(WorkSectionVO.builder().id("8").visible(true).build()));
        assertThat(service.publish(9L).getStatus()).isEqualTo("PUBLISHED");
        verify(content).validatePublication(9L);
    }

    @Test
    void hiddenOrInvalidLegacyBlocksStillFailTheirExistingPublicationRules() {
        WorkEntity work = work("DRAFT", null);
        when(works.byIdForUpdate(9L)).thenReturn(work);
        when(content.sections(9L, false)).thenReturn(List.of(WorkSectionVO.builder().id("8").visible(false).build()));
        doThrow(new WorkStateInvalidException("需要可见区块")).when(content).validatePublication(9L);
        assertThatThrownBy(() -> service.publish(9L)).isInstanceOf(WorkStateInvalidException.class)
                .hasMessageContaining("需要可见区块");
    }

    @Test
    void missingTitleOrInvalidTypeCannotPublish() {
        WorkEntity work = work("DRAFT", null);
        when(works.byIdForUpdate(9L)).thenReturn(work);
        work.setTitle("  ");
        assertThatThrownBy(() -> service.publish(9L)).isInstanceOf(WorkStateInvalidException.class);
        work.setTitle("作品"); work.setWorkType("INVALID");
        assertThatThrownBy(() -> service.publish(9L)).isInstanceOf(WorkStateInvalidException.class);
    }

    @Test
    void unsafeStoredLinksAndPrivateMarkdownMediaStillPreventPublication() {
        when(works.byIdForUpdate(9L)).thenReturn(work("DRAFT", null));
        WorkLinkEntity unsafe = new WorkLinkEntity(); unsafe.setUrl("javascript:alert(1)");
        when(links.listByWorkId(9L)).thenReturn(List.of(unsafe));
        assertThatThrownBy(() -> service.publish(9L)).isInstanceOf(WorkInvalidException.class);
        when(links.listByWorkId(9L)).thenReturn(List.of());
        doThrow(new WorkInvalidException("正文引用私有媒体")).when(references).assertPublicContent(any());
        assertThatThrownBy(() -> service.publish(9L)).isInstanceOf(WorkInvalidException.class)
                .hasMessageContaining("正文引用私有媒体");
    }

    @Test
    void updatingBodyKeepsTheExistingWorkRecord() {
        when(works.byIdForUpdate(9L)).thenReturn(work("DRAFT", null));
        service.updateBody(9L, "# 修改正文");
        verify(works).updateBody(any());
        verify(works, never()).insert(any());
    }
    /**
     * 内容。
     * 协作类型：<code>PortfolioContentService</code>。
     */
    private final PortfolioContentService content = mock(PortfolioContentService.class);
    /**
     * 作品列表。
     * 协作类型：<code>WorkMapper</code>。
     */
    private final WorkMapper works = mock(WorkMapper.class);
    /**
     * 详情。
     * 协作类型：<code>WorkDetailMapper</code>。
     */
    private final WorkDetailMapper details = mock(WorkDetailMapper.class);
    /**
     * 媒体。
     * 协作类型：<code>WorkMediaMapper</code>。
     */
    private final WorkMediaMapper media = mock(WorkMediaMapper.class);
    /**
     * links。
     * 协作类型：<code>WorkLinkMapper</code>。
     */
    private final WorkLinkMapper links = mock(WorkLinkMapper.class);
    /**
     * assets。
     * 协作类型：<code>MediaAssetApi</code>。
     */
    private final MediaAssetApi assets = mock(MediaAssetApi.class);
    /**
     * references。
     * 协作类型：<code>MediaReferenceApi</code>。
     */
    private final MediaReferenceApi references = mock(MediaReferenceApi.class);
    /**
     * 当前操作对应的身份对象。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi actor = mock(CurrentActorApi.class);
    /**
     * 事件集合。
     */
    private final WorkEventPublisher events = mock(WorkEventPublisher.class);
    /**
     * 业务服务。
     */
    private final PortfolioWorkServiceImpl service = new PortfolioWorkServiceImpl(
            content, works, details, media, links, assets, references, actor, new ObjectMapper(),
            List.of(new OtherDetailValidator()), events);

    /**
     * 操作者是否Available处理入口。
     * 主要协作调用：<code>content.taxonomy</code>、<code>media.listByWorkId</code>、<code>assets.get</code>、<code>actor.current</code>。
     */
    @BeforeEach
    void actorIsAvailable() {
        when(content.taxonomy()).thenReturn(java.util.Map.of("categories", List.of(), "formats", List.of(), "tags", List.of()));
        WorkMediaEntity cover = new WorkMediaEntity(); cover.setId(3L); cover.setSortOrder(0); cover.setWorkId(9L); cover.setMediaAssetId(77L); cover.setUsageType("COVER");
        when(media.listByWorkId(any())).thenReturn(List.of(cover));
        when(assets.get(77L)).thenReturn(com.starrainnotes.media.api.dto.MediaAssetSummary.builder().id(77L).mediaType("IMAGE").accessLevel("PUBLIC").status("ACTIVE").build());
        when(actor.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(7L).build());
    }

    /**
     * patchCanExplicitlyClearOptionalDates处理入口。
     * 主要协作调用：<code>works.byIdForUpdate</code>、<code>service.update</code>。
     */
    @Test
    void patchCanExplicitlyClearOptionalDates() {
        WorkEntity work = work("DRAFT", null);
        work.setStartedOn(java.time.LocalDate.of(2026, 1, 1));
        work.setEndedOn(java.time.LocalDate.of(2026, 2, 1));
        when(works.byIdForUpdate(9L)).thenReturn(work);
        service.update(9L, com.starrainnotes.portfolio.dto.WorkPatchDTO.builder()
                .clearStartedOn(true).clearEndedOn(true).build());
        assertThat(work.getStartedOn()).isNull();
        assertThat(work.getEndedOn()).isNull();
        verify(works).updateContent(work);
    }

    /**
     * 已撤回作品Restores不含Changing首次发布时间处理入口。
     * 主要协作调用：<code>works.byIdForUpdate</code>、<code>details.selectByWorkId</code>、<code>links.listByWorkId</code>、<code>service.restore</code>。
     */
    @Test
    void withdrawnWorkRestoresWithoutChangingFirstPublicationTime() {
        LocalDateTime firstPublished = LocalDateTime.of(2026, 9, 1, 10, 30);
        WorkEntity work = work("WITHDRAWN", firstPublished);
        WorkDetailEntity detail = new WorkDetailEntity();
        detail.setDetailJson("{}");
        when(works.byIdForUpdate(9L)).thenReturn(work);
        when(details.selectByWorkId(any())).thenReturn(detail);
        when(links.listByWorkId(any())).thenReturn(List.of());

        assertThat(service.restore(9L).getStatus()).isEqualTo("PUBLISHED");
        assertThat(work.getPublishedAt()).isEqualTo(firstPublished);
        verify(events).publish(any());
    }

    /**
     * publishing不含类型详情是否Allowed处理入口。
     * 主要协作调用：<code>works.byIdForUpdate</code>、<code>links.listByWorkId</code>、<code>service.publish</code>。
     */
    @Test
    void publishingWithoutTypeDetailIsAllowed() {
        WorkEntity work = work("DRAFT", null);
        when(works.byIdForUpdate(9L)).thenReturn(work);
        when(links.listByWorkId(any())).thenReturn(List.of());

        // 原来的规则是「发布前必须填写类型详情」；作品后台整块去掉类型详情后，
        // 这条断言反过来：缺详情不再阻断发布。
        assertThat(service.publish(9L).getStatus()).isEqualTo("PUBLISHED");
        assertThat(work.getStatus()).isEqualTo("PUBLISHED");
    }

    /**
     * 公开查找Does不RevealUnpublished作品处理入口。
     * 主要协作调用：<code>service.publicWork</code>。
     */
    @Test
    void publicLookupDoesNotRevealUnpublishedWork() {
        assertThatThrownBy(() -> service.publicWork("private-draft"))
                .isInstanceOf(WorkNotFoundException.class);
    }

    /**
     * removing媒体DetachesIts引用处理入口。
     * 主要协作调用：<code>media.selectById</code>、<code>works.byIdForUpdate</code>、<code>service.removeMedia</code>。
     */
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

    /**
     * 已发布作品MustBe已撤回之前Deletion处理入口。
     * 主要协作调用：<code>works.byIdForUpdate</code>、<code>service.delete</code>。
     */
    @Test
    void publishedWorkMustBeWithdrawnBeforeDeletion() {
        when(works.byIdForUpdate(9L)).thenReturn(work("PUBLISHED", LocalDateTime.now()));
        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(WorkStateInvalidException.class);
    }

    /**
     * 作品处理入口。
     * 关键处理步骤调用 <code>work.setId</code>、<code>work.setSlug</code>、<code>work.setWorkType</code>、<code>work.setTitle</code>。
     *
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @param publishedAt 已发布时间；类型为 <code>LocalDateTime</code>
     * @return 类型为 <code>WorkEntity</code> 的处理结果
     */
    private WorkEntity work(String status, LocalDateTime publishedAt) {
        WorkEntity work = new WorkEntity();
        work.setId(9L);
        work.setSlug("sample-work");
        work.setWorkType("OTHER");
        work.setTitle("Sample");
        work.setSummary("A meaningful case study");
        work.setCategoryId(1L); work.setFormatId(1L);
        work.setBodyMarkdown("# Case Study");
        work.setStatus(status);
        work.setPublishedAt(publishedAt);
        return work;
    }
}
