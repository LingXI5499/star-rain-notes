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
public final class ReviewAccountLabels {

    private ReviewAccountLabels() {
    }

    public static String of(Long accountId) {
        return accountId == null ? null : "账户 #" + accountId;
    }

    // 决策人展示：审核请求只保存 reviewer_account_id，这里给出前端一致的呈现文案
    public static String reviewerLabel(Long reviewerAccountId) {
        return reviewerAccountId == null ? null : "审核员 #" + reviewerAccountId;
    }
}
