package com.starrainnotes.review.api;

/*
 * 目标业务模块提供的「不可变审核视图」。
 *
 * Review 刻意不认识 TutorialEntity / BlogPostEntity：它只把这个接口原样
 * 放进审核详情响应里。业务模块自己决定暴露哪些字段用于审核，
 * Review 不解析也不校验其中的业务规则。
 *
 * 实现类示例：TutorialReviewView、BlogReviewView。
 */
public interface ReviewTargetView {

    // 视图类型标识，例如 TUTORIAL_CHAPTER；前端据此选择渲染方式
    String viewType();
}
