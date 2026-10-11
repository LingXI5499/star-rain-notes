/*
 * TutorialPublicationChangedEvent：教程与学习模块的跨模块接口文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */

package com.starrainnotes.tutorial.api.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 教程发布Changed事件，属于教程与学习模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Data
@AllArgsConstructor
public class TutorialPublicationChangedEvent {
    /**
     * 教程标识。
     */
    private Long tutorialId;
    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 标题。
     */
    private String title;
    /**
     * 动作。
     */
    private String action;
    /**
     * 已发布时间。
     */
    private LocalDateTime publishedAt;
}
