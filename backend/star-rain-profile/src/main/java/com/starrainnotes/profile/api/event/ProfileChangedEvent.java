/*
 * ProfileChangedEvent：作者资料模块的跨模块接口文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */

package com.starrainnotes.profile.api.event;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 资料Changed事件，属于作者资料模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Data
@AllArgsConstructor
public class ProfileChangedEvent {
    /**
     * 资料标识。
     */
    private Long profileId;
}
