/*
 * ReviewDemoRevisions：审核模块的测试文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 阅读入口：contentFor（内容面向处理入口）；frozenAt（frozen时间处理入口）；displayNameFor（显示名称面向处理入口）。
 */

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
/**
 * 审核Demo修订集合，属于审核模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
public final class ReviewDemoRevisions {

    /**
     * 构造ReviewDemoRevisions，接收本类使用的依赖或初始化参数。
     */
    private ReviewDemoRevisions() {
    }

    // 由 revisionRef 推导出一个稳定可见的内容快照
    /**
     * 内容面向处理入口。
     * 关键处理步骤调用 <code>revisionRef.isBlank</code>。
     *
     * @param revisionRef 版本或修订的引用值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String contentFor(String revisionRef) {
        if (revisionRef == null || revisionRef.isBlank()) {
            return null;
        }
        return "演示版本快照：" + revisionRef + "\n"
                + "该内容在提交审核时冻结，Reviewer 看到的就是这一份。";
    }

    // 供演示视图标注冻结时间；真实实现应使用版本表里的创建时间
    /**
     * frozen时间处理入口。
     * 关键处理步骤调用 <code>LocalDateTime.now</code>。
     * @return 类型为 <code>LocalDateTime</code> 的处理结果
     */
    public static LocalDateTime frozenAt() {
        return LocalDateTime.now();
    }

    // 目标显示名快照的兜底文案
    /**
     * 显示名称面向处理入口。
     *
     * @param targetType 目标对象的业务类型；类型为 <code>String</code>
     * @param targetId 被处理或审核的目标对象标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String displayNameFor(String targetType, Long targetId) {
        return "演示" + (targetType == null ? "目标" : targetType) + " #" + targetId;
    }
}
