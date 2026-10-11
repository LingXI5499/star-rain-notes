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
public final class ReviewTargetKeys {

    private ReviewTargetKeys() {
    }

    // 命名锁前缀：与 MySQL 实例里其他用途的命名锁隔开
    private static final String LOCK_PREFIX = "sr_review_pending:";

    public static String lockName(ReviewTargetKey target, String reviewType) {
        String identity = describe(target, reviewType);
        // 用 String.hashCode 不够稳（碰撞概率高），这里用 32 位 FNV-1a 手工实现，
        // 不引入额外依赖也能得到分布均匀的定长键
        return LOCK_PREFIX + String.format("%08x", fnv1a32(identity));
    }

    public static String describe(ReviewTargetKey target, String reviewType) {
        if (target == null) {
            return "null/" + reviewType;
        }
        return target.getTargetModule() + "/" + target.getTargetType() + "/"
                + target.getTargetId() + "/" + reviewType;
    }

    private static int fnv1a32(String value) {
        int hash = 0x811c9dc5;
        for (int index = 0; index < value.length(); index++) {
            hash ^= value.charAt(index);
            hash *= 0x01000193;
        }
        return hash;
    }
}
