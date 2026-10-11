/*
 * ReviewViewerProviderImpl：审核模块的业务实现文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：CurrentActorApi、ReviewViewer、ReviewViewerProvider。
 * 阅读入口：current（当前处理入口）。
 */

package com.starrainnotes.review.service.impl;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.review.context.ReviewViewer;
import com.starrainnotes.review.service.ReviewViewerProvider;
import org.springframework.stereotype.Service;

/*
 * 从 Account 的 CurrentActorApi 构造 ReviewViewer。
 *
 * Review 只依赖 Account 的 api 包，不碰 AccountPrincipal 这类模块内部类型，
 * 因此 Account 将来重构会话实现时 Review 不受影响。
 *
 * 未登录时 CurrentActorApi.current() 会抛 UNAUTHORIZED（401），
 * 这与 URL 层的 authenticated() 是同一语义，不重复处理。
 */
/**
 * 审核Viewer提供方实现，属于审核模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
public class ReviewViewerProviderImpl implements ReviewViewerProvider {

    /**
     * 提供当前认证操作者身份的跨模块接口。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi currentActorApi;

    /**
     * 构造ReviewViewerProviderImpl，接收本类使用的依赖或初始化参数。
     *
     * @param currentActorApi 提供当前认证操作者身份的跨模块接口；类型为 <code>CurrentActorApi</code>
     */
    public ReviewViewerProviderImpl(CurrentActorApi currentActorApi) {
        this.currentActorApi = currentActorApi;
    }

    /**
     * 当前处理入口。
     * 主要协作调用：<code>currentActorApi.current</code>。
     * @return 类型为 <code>ReviewViewer</code> 的处理结果
     */
    @Override
    public ReviewViewer current() {
        CurrentActorApi.CurrentActor actor = currentActorApi.current();
        return ReviewViewer.builder()
                .accountId(actor.getAccountId())
                .permissions(actor.getPermissions())
                .build();
    }
}
