/*
 * TutorialMediaReferences：教程与学习模块的模块支持文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：TutorialQuestionMapper、TutorialKnowledgeCardMapper、MediaReferenceApi、MediaReferenceCommand、MediaUsageCodes、TutorialContentMediaParser。
 * 阅读入口：replaceChapter（替换章节）；attachRevision（建立关联修订）；detachChapter（解除关联章节）；detachRevision（解除关联修订）。
 */

package com.starrainnotes.tutorial.content.media;

import com.starrainnotes.tutorial.content.mapper.TutorialQuestionMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialKnowledgeCardMapper;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.api.constant.MediaUsageCodes;
import com.starrainnotes.tutorial.content.utils.TutorialContentMediaParser;
import java.util.Collection;
import java.util.LinkedHashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 教程媒体References，属于教程与学习模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class TutorialMediaReferences {
    public void assertPublicContent(String markdown) { mediaReferenceApi.assertPublicContent(markdown); }
    /**
     * MODULE。
     */
    private static final String MODULE = "TUTORIAL";
    /**
     * 章节。
     */
    private static final String CHAPTER = "CHAPTER";
    /**
     * 修订。
     */
    private static final String REVISION = "REVISION";
    /**
     * 媒体引用接口。
     * 协作类型：<code>MediaReferenceApi</code>。
     */
    private final MediaReferenceApi mediaReferenceApi;
    /**
     * 卡片数据映射。
     * 协作类型：<code>TutorialKnowledgeCardMapper</code>。
     */
    private final TutorialKnowledgeCardMapper cardMapper;
    /**
     * 问题数据映射。
     * 协作类型：<code>TutorialQuestionMapper</code>。
     */
    private final TutorialQuestionMapper questionMapper;

    /**
     * 替换章节。
     * 主要协作调用：<code>mediaReferenceApi.detachAll</code>、<code>cardMapper.listByChapterId</code>、<code>questionMapper.listByChapterId</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param markdown Markdown；类型为 <code>String</code>
     */
    public void replaceChapter(Long chapterId, String markdown) {
        mediaReferenceApi.detachAll(MODULE, CHAPTER, chapterId);
        LinkedHashSet<Long> assets = new LinkedHashSet<>(TutorialContentMediaParser.extractMediaAssetIds(markdown));
        cardMapper.listByChapterId(chapterId).forEach(card -> assets.addAll(TutorialContentMediaParser.extractMediaAssetIds(card.getBackMarkdown())));
        questionMapper.listByChapterId(chapterId).forEach(question -> assets.addAll(TutorialContentMediaParser.extractMediaAssetIds(question.getReferenceAnswer())));
        attach(CHAPTER, chapterId, assets);
    }

    /**
     * 建立关联修订。
     * 关键处理步骤调用 <code>assetIds.addAll</code>、<code>TutorialContentMediaParser.extractMediaAssetIds</code>、<code>attach</code>。
     *
     * @param revisionId 修订标识；类型为 <code>Long</code>
     * @param chapterBodies 章节Bodies；类型为 <code>Collection&lt;String&gt;</code>
     */
    public void attachRevision(Long revisionId, Collection<String> chapterBodies) {
        LinkedHashSet<Long> assetIds = new LinkedHashSet<>();
        for (String body : chapterBodies) {
            assetIds.addAll(TutorialContentMediaParser.extractMediaAssetIds(body));
        }
        attach(REVISION, revisionId, assetIds);
    }

    /**
     * 解除关联章节。
     * 主要协作调用：<code>mediaReferenceApi.detachAll</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     */
    public void detachChapter(Long chapterId) {
        mediaReferenceApi.detachAll(MODULE, CHAPTER, chapterId);
    }

    /**
     * 解除关联修订。
     * 主要协作调用：<code>mediaReferenceApi.detachAll</code>。
     *
     * @param revisionId 修订标识；类型为 <code>Long</code>
     */
    public void detachRevision(Long revisionId) {
        mediaReferenceApi.detachAll(MODULE, REVISION, revisionId);
    }

    /**
     * 建立关联教程媒体References。
     * 主要协作调用：<code>mediaReferenceApi.attach</code>。
     *
     * @param sourceType 来源对象的业务类型；类型为 <code>String</code>
     * @param sourceId 来源对象的业务标识；类型为 <code>Long</code>
     * @param assetIds 资产标识集合；类型为 <code>Collection&lt;Long&gt;</code>
     */
    private void attach(String sourceType, Long sourceId, Collection<Long> assetIds) {
        for (Long mediaAssetId : assetIds) {
            mediaReferenceApi.attach(MediaReferenceCommand.builder()
                    .mediaAssetId(mediaAssetId)
                    .sourceModule(MODULE)
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .usageCode(MediaUsageCodes.TUTORIAL_CHAPTER_CONTENT)
                    .build());
        }
    }
}
