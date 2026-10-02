package com.starrainnotes.review.enumeration;

/*
 * 动作执行者类型。
 *
 * SYSTEM 预留给「目标业务对象被删除，系统自动取消待审请求」这类无自然人的动作，
 * 此时 actor_account_id 允许为空。
 */
public enum ReviewActorType {
    ACCOUNT,
    SYSTEM;

    public static final String ACCOUNT_CODE = "ACCOUNT";
    public static final String SYSTEM_CODE = "SYSTEM";
}
