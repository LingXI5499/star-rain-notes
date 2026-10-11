/*
 * ReviewTargetKeys：审核模块的工具文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 主要协作类型：ReviewTargetKey。
 * 阅读入口：lockName（锁名称处理入口）；describe（describe处理入口）。
 */

package com.starrainnotes.review.utils;

import com.starrainnotes.review.api.ReviewTargetKey;

/*
 * 审核目标维度的稳定标识工具。
 *
 * 两个用途：
 *   1. 生成「同一目标 + reviewType」的命名锁名。命名锁名长度有限（MySQL 上限 64 字符），
 *      而 targetModule/targetType/reviewType 组合可能很长，所以这里做定长哈希，
 *      既保证同一目标得到同一个锁名，也保证不会超长。
 *   2. 生成诊断用的可读描述，便于日志与错误排查。
 */
/**
 * 审核目标Keys，属于审核模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class ReviewTargetKeys {

    /**
     * 构造ReviewTargetKeys，接收本类使用的依赖或初始化参数。
     */
    private ReviewTargetKeys() {
    }

    // 命名锁前缀：与 MySQL 实例里其他用途的命名锁隔开
    /**
     * PREFIX。
     */
    private static final String LOCK_PREFIX = "sr_review_pending:";

    /**
     * 锁名称处理入口。
     * 关键处理步骤调用 <code>describe</code>、<code>String.format</code>、<code>fnv1a32</code>。
     *
     * @param target 目标；类型为 <code>ReviewTargetKey</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String lockName(ReviewTargetKey target, String reviewType) {
        String identity = describe(target, reviewType);
        // 用 String.hashCode 不够稳（碰撞概率高），这里用 32 位 FNV-1a 手工实现，
        // 不引入额外依赖也能得到分布均匀的定长键
        return LOCK_PREFIX + String.format("%08x", fnv1a32(identity));
    }

    /**
     * describe处理入口。
     * 关键处理步骤调用 <code>target.getTargetModule</code>、<code>target.getTargetType</code>、<code>target.getTargetId</code>。
     * 边界判断包含 <code>(target == null)</code>。
     *
     * @param target 目标；类型为 <code>ReviewTargetKey</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String describe(ReviewTargetKey target, String reviewType) {
        if (target == null) {
            return "null/" + reviewType;
        }
        return target.getTargetModule() + "/" + target.getTargetType() + "/"
                + target.getTargetId() + "/" + reviewType;
    }

    /**
     * fnv1a32处理入口。
     * 关键处理步骤调用 <code>value.length</code>、<code>value.charAt</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    private static int fnv1a32(String value) {
        int hash = 0x811c9dc5;
        for (int index = 0; index < value.length(); index++) {
            hash ^= value.charAt(index);
            hash *= 0x01000193;
        }
        return hash;
    }
}
