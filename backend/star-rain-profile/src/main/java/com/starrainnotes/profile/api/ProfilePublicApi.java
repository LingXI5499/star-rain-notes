/*
 * ProfilePublicApi：作者资料模块的跨模块接口文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：ProfileVO、ProfilePublishedDocument。
 * 阅读入口：summary（摘要处理入口）；publicProfileId（公开资料标识处理入口）；publishedDocument（已发布文档处理入口）。
 */

package com.starrainnotes.profile.api;

import com.starrainnotes.profile.api.vo.ProfileVO;
import com.starrainnotes.profile.api.dto.ProfilePublishedDocument;

/**
 * 资料公开接口，属于作者资料模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface ProfilePublicApi {
    /**
     * 摘要处理入口。
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    ProfileVO summary();
    /**
     * 公开资料标识处理入口。
     * @return 类型为 <code>Long</code> 的处理结果
     */
    Long publicProfileId();
    /**
     * 已发布文档处理入口。
     * @return 类型为 <code>ProfilePublishedDocument</code> 的处理结果
     */
    ProfilePublishedDocument publishedDocument();
}
