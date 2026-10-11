/*
 * ReviewViewer：审核模块的模块支持文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：ReviewPermissions。
 * 阅读入口：hasPermission（判断是否具备权限）；reviewer（审核人处理入口）；isApplicant（判断是否申请人）。
 */

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
/**
 * 审核Viewer，属于审核模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewViewer {

    /**
     * 账号标识。
     */
    private Long accountId;

    /**
     * 权限集合。
     */
    private Set<String> permissions;

    /**
     * 判断是否具备权限。
     * 主要协作调用：<code>permissions.contains</code>。
     *
     * @param permission 权限；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    // 是否具备审核查看权限（review:read）
    /**
     * 审核人处理入口。
     * 关键处理步骤调用 <code>hasPermission</code>。
     * @return 本方法条件是否成立的判断结果
     */
    public boolean reviewer() {
        return hasPermission(ReviewPermissions.READ);
    }

    // 是否就是这条请求的申请人
    /**
     * 判断是否申请人。
     * 主要协作调用：<code>accountId.equals</code>。
     *
     * @param applicantAccountId 申请人账号标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    public boolean isApplicant(Long applicantAccountId) {
        return accountId != null && accountId.equals(applicantAccountId);
    }
}
