/*
 * WorkPublicationChangedEvent：作品模块的跨模块接口文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */

package com.starrainnotes.portfolio.api.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作品发布Changed事件，属于作品模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkPublicationChangedEvent {
    /**
     * 作品标识。
     */
    private Long workId;
    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 已发布。
     */
    private boolean published;
}
