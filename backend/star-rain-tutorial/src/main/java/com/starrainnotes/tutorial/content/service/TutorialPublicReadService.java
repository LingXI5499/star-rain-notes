/*
 * TutorialPublicReadService：教程与学习模块的业务契约文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 阅读入口：tutorial（教程处理入口）；chapter（章节处理入口）。
 */

package com.starrainnotes.tutorial.content.service;

import com.fasterxml.jackson.databind.JsonNode;

// 教程公开阅读入口：读取已发布的教程与章节并记录浏览事件。
/**
 * 教程公开读取业务服务，属于教程与学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface TutorialPublicReadService {

    /**
     * 教程处理入口。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    JsonNode tutorial(String slug);

    /**
     * 章节处理入口。
     *
     * @param tutorialSlug 教程固定链接标识；类型为 <code>String</code>
     * @param chapterSlug 章节固定链接标识；类型为 <code>String</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     */
    JsonNode chapter(String tutorialSlug, String chapterSlug);
}
