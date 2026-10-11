/*
 * TutorialPublicReadServiceImpl：教程与学习模块的业务实现文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：AnalyticsRecordApi、ContentViewEvent、TutorialPublicReadService、TutorialPublicationService。
 * 阅读入口：tutorial（教程处理入口）；chapter（章节处理入口）。
 */

package com.starrainnotes.tutorial.content.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.analytics.api.AnalyticsRecordApi;
import com.starrainnotes.analytics.api.ContentViewEvent;
import com.starrainnotes.tutorial.content.service.TutorialPublicReadService;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 教程公开读取业务服务实现，属于教程与学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class TutorialPublicReadServiceImpl implements TutorialPublicReadService {
    /**
     * publications。
     * 协作类型：<code>TutorialPublicationService</code>。
     */
    private final TutorialPublicationService publications;
    /**
     * 统计。
     */
    private final ObjectProvider<AnalyticsRecordApi> analytics;

    /**
     * 教程处理入口。
     * 主要协作调用：<code>publications.publicTutorial</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    @Override
    public JsonNode tutorial(String slug) {
        JsonNode detail = publications.publicTutorial(slug);
        record("TUTORIAL", detail, "/tutorials/:slug");
        return detail;
    }

    /**
     * 章节处理入口。
     * 主要协作调用：<code>publications.publicChapter</code>。
     *
     * @param tutorialSlug 教程固定链接标识；类型为 <code>String</code>
     * @param chapterSlug 章节固定链接标识；类型为 <code>String</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    @Override
    public JsonNode chapter(String tutorialSlug, String chapterSlug) {
        JsonNode detail = publications.publicChapter(tutorialSlug, chapterSlug);
        record("CHAPTER", detail, "/tutorials/:slug/:chapterSlug");
        return detail;
    }

    /**
     * 记录教程公开读取。
     * 主要协作调用：<code>analytics.getIfAvailable</code>。
     *
     * @param type 类型；类型为 <code>String</code>
     * @param detail 详情；类型为 <code>JsonNode</code>
     * @param route 路由；类型为 <code>String</code>
     */
    private void record(String type, JsonNode detail, String route) {
        AnalyticsRecordApi recorder = analytics.getIfAvailable();
        if (recorder != null)
            recorder.recordContentView(new ContentViewEvent(type, detail.path("id").asLong(), route));
    }
}
