package com.starrainnotes.review.context;

import com.starrainnotes.review.constant.ReviewPermissions;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 审核详情的查看者。
 *
 * REV-003 的对象级授权靠它判定，规则是「满足至少之一」：
 *   1. actor.accountId == applicantAccountId（申请人看自己的申请）；
 *   2. actor 拥有 review:read（Reviewer / 后台管理者）。
 * 否则属于 IDOR 风险，Service 会抛 REVIEW_ACCESS_DENIED。
 *
 * 把权限集合与账户 ID 打包成一个值对象，是为了让授权判断只有一个入口，
 * 不散落在 Controller 与 Service 的多个 if 里。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewViewer {

    private Long accountId;

    private Set<String> permissions;

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    // 是否具备审核查看权限（review:read）
    public boolean reviewer() {
        return hasPermission(ReviewPermissions.READ);
    }

    // 是否就是这条请求的申请人
    public boolean isApplicant(Long applicantAccountId) {
        return accountId != null && accountId.equals(applicantAccountId);
    }
}
