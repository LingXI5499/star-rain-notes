/*
 * ReviewViewerProvider：审核模块的业务契约文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：ReviewViewer。
 * 阅读入口：current（当前处理入口）。
 */

package com.starrainnotes.review.service;

import com.starrainnotes.review.context.ReviewViewer;

/*
 * 当前认证主体到 ReviewViewer 的唯一转换入口。
 *
 * 单独抽一个接口是为了让「谁在看我这条请求」这件事只有一处实现：
 * Controller 不自己拼 ReviewViewer，测试也只需要替换这一个 Bean，
 * 就能构造出「申请人本人 / 无关用户 / Reviewer」三种主体。
 */
/**
 * 审核Viewer提供方，属于审核模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface ReviewViewerProvider {

    /**
     * 当前处理入口。
     * @return 类型为 <code>ReviewViewer</code> 的处理结果
     */
    ReviewViewer current();
}
