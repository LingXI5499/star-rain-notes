/*
 * AccountReferenceApi：账号模块的跨模块接口文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 阅读入口：exists（判断是否存在账号引用）；isActive（判断是否有效）；displayName（显示名称处理入口）。
 */

package com.starrainnotes.account.api;

/*
 * 其他模块检查账户逻辑引用的唯一入口。
 */
/**
 * 账号引用接口，属于账号模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface AccountReferenceApi {

    /**
     * 判断是否存在账号引用。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    boolean exists(Long accountId);

    /**
     * 判断是否有效。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    boolean isActive(Long accountId);

    /**
     * 显示名称处理入口。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    String displayName(Long accountId);
}

