package com.starrainnotes.review.exception;

import com.starrainnotes.common.exception.ApiException;

/*
 * 目标业务对象不存在（已被删除）。
 *
 * 错误码 REVIEW_TARGET_NOT_FOUND，HTTP 404。
 *
 * 与 REVIEW_TARGET_REVISION_NOT_FOUND 的分工：
 *   本条用于「对象本身没了」，那条用于「对象还在、提交时冻结的版本读不到了」。
 *   业务模块通过 loadReviewView 返回 null 表达「读不到」，
 *   因此 Review 侧当前统一按 REVISION_NOT_FOUND 报出；
 *   本错误码保留给将来业务模块显式区分「对象已删除」的场景，
 *   以免两种语义被迫挤进同一个码里，前端无法给出不同引导。
 */
public class ReviewTargetNotFoundException extends ApiException {

    public ReviewTargetNotFoundException() {
        super("REVIEW_TARGET_NOT_FOUND", "审核目标不存在或已被删除", 404);
    }
}
