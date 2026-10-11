/*
 * ReviewAccountLabels：审核模块的工具文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 阅读入口：of（所属处理入口）；reviewerLabel（审核人标签处理入口）。
 */

package com.starrainnotes.review.utils;

/*
 * 账户展示标签。
 *
 * Review 只从 Account 的 CurrentActorApi 拿到 accountId / roles / permissions，
 * 没有 displayName：Account 模块尚未向其他模块暴露账户摘要 API。
 * 因此申请人显示名在提交时只能落一个稳定可读的标签，
 * 前端拿到的 applicantDisplayName 就是这个值，不会出现 null。
 *
 * Account 将来提供账户摘要 API 后，把这里替换成真实 displayName 即可，
 * 数据库列与前端展示都不需要改。
 */
/**
 * 审核账号Labels，属于审核模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class ReviewAccountLabels {

    /**
     * 构造ReviewAccountLabels，接收本类使用的依赖或初始化参数。
     */
    private ReviewAccountLabels() {
    }

    /**
     * 所属处理入口。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String of(Long accountId) {
        return accountId == null ? null : "账户 #" + accountId;
    }

    // 决策人展示：审核请求只保存 reviewer_account_id，这里给出前端一致的呈现文案
    /**
     * 审核人标签处理入口。
     *
     * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String reviewerLabel(Long reviewerAccountId) {
        return reviewerAccountId == null ? null : "审核员 #" + reviewerAccountId;
    }
}
