/*
 * AccountAuditService：账号模块的业务契约文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 阅读入口：success（success处理入口）；failed（失败处理入口）；failedIndependently（失败独立执行处理入口）。
 */

package com.starrainnotes.account.service;

/*
 * 账户审计流水写入入口。
 *
 * 这里有三个写入方法，区别只在**事务归属**，业务含义都是「记一条审计」：
 *   success / failed —— 加入调用方事务。调用方回滚，这条审计也回滚，
 *                       适用于「审计描述的是本次事务内的动作」。
 *   failedIndependently —— 独立事务，立即提交，不受调用方回滚影响。
 *                       适用于「审计描述的是本次事务失败这件事本身」：
 *                       如果它也加入调用方事务，就会随着失败一起消失，
 *                       而失败审计恰恰是排查问题时要留下的东西。
 *
 * 引入 failedIndependently 是因为「邮件投递失败要留审计」这条规则原先只能写在 Controller 里：
 * 在 Controller 的 catch 中调用 failed() 时服务事务已经回滚，写审计落到了自动提交上、能活下来；
 * 一旦把它搬进 @Transactional 的服务方法，用 failed() 就会丢审计。
 * 现在服务层有了语义明确的方法，这条规则可以放回它该在的地方，不必依赖调用时机。
 */
/**
 * 账号审计业务服务，属于账号模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface AccountAuditService {

    /**
     * success处理入口。
     *
     * @param actor 当前操作对应的身份对象；类型为 <code>Long</code>
     * @param target 目标；类型为 <code>Long</code>
     * @param action 动作；类型为 <code>String</code>
     */
    void success(Long actor, Long target, String action);

    /**
     * 失败处理入口。
     *
     * @param actor 当前操作对应的身份对象；类型为 <code>Long</code>
     * @param target 目标；类型为 <code>Long</code>
     * @param action 动作；类型为 <code>String</code>
     */
    void failed(Long actor, Long target, String action);

    // 独立事务写入：调用方回滚也保留这条记录
    /**
     * 失败独立执行处理入口。
     *
     * @param actor 当前操作对应的身份对象；类型为 <code>Long</code>
     * @param target 目标；类型为 <code>Long</code>
     * @param action 动作；类型为 <code>String</code>
     */
    void failedIndependently(Long actor, Long target, String action);
}
