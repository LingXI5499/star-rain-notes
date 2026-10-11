/*
 * BlogPublicReadService：博客模块的业务契约文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：BlogPostPublicDetailVO。
 * 阅读入口：read（读取博客公开读取）。
 */

package com.starrainnotes.blog.service;

import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;

// 博客公开阅读入口：读取文章详情并记录浏览事件。
/**
 * 博客公开读取业务服务，属于博客模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface BlogPublicReadService {

    /**
     * 读取博客公开读取。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>BlogPostPublicDetailVO</code> 的处理结果
     */
    BlogPostPublicDetailVO read(String slug);
}
