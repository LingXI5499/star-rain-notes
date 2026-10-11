/*
 * ReviewActorType：审核模块的枚举文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */

package com.starrainnotes.review.enumeration;

/*
 * 动作执行者类型。
 *
 * SYSTEM 预留给「目标业务对象被删除，系统自动取消待审请求」这类无自然人的动作，
 * 此时 actor_account_id 允许为空。
 */
/**
 * 审核操作者类型，属于审核模块。
 * 集中定义有限的业务取值及其附加元数据，调用方通过枚举方法完成代码、标签或状态转换。
 */
public enum ReviewActorType {
    /**
     * 账号。
     */
    ACCOUNT,
    /**
     * SYSTEM。
     */
    SYSTEM;

    /**
     * 代码。
     */
    public static final String ACCOUNT_CODE = "ACCOUNT";
    /**
     * 代码。
     */
    public static final String SYSTEM_CODE = "SYSTEM";
}
