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
public interface AccountAuditService {

    void success(Long actor, Long target, String action);

    void failed(Long actor, Long target, String action);

    // 独立事务写入：调用方回滚也保留这条记录
    void failedIndependently(Long actor, Long target, String action);
}
