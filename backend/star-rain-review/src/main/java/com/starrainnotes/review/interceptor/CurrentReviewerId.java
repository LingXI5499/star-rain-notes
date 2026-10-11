/*
 * CurrentReviewerId：审核模块的模块支持文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */

package com.starrainnotes.review.interceptor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * 把当前认证账户ID注入 Controller 方法参数。
 *
 * 为什么需要它：审核决策必须记录 reviewer_account_id，而这个 ID 只能来自认证上下文。
 * 如果让 Controller 显式调用 CurrentActorApi，每个写接口都要重复一遍取值代码，
 * 也容易在某个新端点上漏掉。用参数解析器统一注入后，
 * 「决策人来自服务端」成为签名层面的事实，浏览器无法通过请求体伪造决策人。
 *
 * 与 AccountPrincipal 的关系：实现里走 Account 的 CurrentActorApi，
 * 不直接读 SecurityContext 的模块内部类型。
 */
/**
 * 当前审核人标识，属于审核模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentReviewerId {
}
