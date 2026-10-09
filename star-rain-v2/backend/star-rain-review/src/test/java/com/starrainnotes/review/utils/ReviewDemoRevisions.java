package com.starrainnotes.review.utils;

import java.time.LocalDateTime;

/*
 * 演示审核目标的版本内容生成器。
 *
 * 真实业务模块的审核视图来自它自己的版本表，不需要这类工具；
 * 演示 handler 没有版本表，所以用「由 revisionRef 确定性推导」的方式代替：
 * 同一个 revisionRef 永远得到同一份快照，符合「不可变审核版本」的语义，
 * 也让演示行为可复现、可断言。
 *
 * 本类只在测试源码中存在。
 */
public final class ReviewDemoRevisions {

    private ReviewDemoRevisions() {
    }

    // 由 revisionRef 推导出一个稳定可见的内容快照
    public static String contentFor(String revisionRef) {
        if (revisionRef == null || revisionRef.isBlank()) {
            return null;
        }
        return "演示版本快照：" + revisionRef + "\n"
                + "该内容在提交审核时冻结，Reviewer 看到的就是这一份。";
    }

    // 供演示视图标注冻结时间；真实实现应使用版本表里的创建时间
    public static LocalDateTime frozenAt() {
        return LocalDateTime.now();
    }

    // 目标显示名快照的兜底文案
    public static String displayNameFor(String targetType, Long targetId) {
        return "演示" + (targetType == null ? "目标" : targetType) + " #" + targetId;
    }
}
