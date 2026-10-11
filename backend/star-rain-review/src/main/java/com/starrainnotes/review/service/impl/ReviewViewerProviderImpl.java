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
@Service
public class ReviewViewerProviderImpl implements ReviewViewerProvider {

    private final CurrentActorApi currentActorApi;

    public ReviewViewerProviderImpl(CurrentActorApi currentActorApi) {
        this.currentActorApi = currentActorApi;
    }

    @Override
    public ReviewViewer current() {
        CurrentActorApi.CurrentActor actor = currentActorApi.current();
        return ReviewViewer.builder()
                .accountId(actor.getAccountId())
                .permissions(actor.getPermissions())
                .build();
    }
}
