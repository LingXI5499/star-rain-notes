package com.starrainnotes.review.service;

import com.starrainnotes.review.context.ReviewViewer;

/*
 * 当前认证主体到 ReviewViewer 的唯一转换入口。
 *
 * 单独抽一个接口是为了让「谁在看我这条请求」这件事只有一处实现：
 * Controller 不自己拼 ReviewViewer，测试也只需要替换这一个 Bean，
 * 就能构造出「申请人本人 / 无关用户 / Reviewer」三种主体。
 */
public interface ReviewViewerProvider {

    ReviewViewer current();
}
