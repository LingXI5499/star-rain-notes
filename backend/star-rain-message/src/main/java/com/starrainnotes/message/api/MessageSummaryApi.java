/*
 * MessageSummaryApi：留言模块的跨模块接口文件。
 * 业务范围：留言提交、审核展示和后台管理。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 阅读入口：pendingCount（待处理数量处理入口）。
 */

package com.starrainnotes.message.api;

/**
 * 留言摘要接口，属于留言模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface MessageSummaryApi {
    /**
     * 待处理数量处理入口。
     * @return 类型为 <code>long</code> 的处理结果
     */
    long pendingCount();
}
