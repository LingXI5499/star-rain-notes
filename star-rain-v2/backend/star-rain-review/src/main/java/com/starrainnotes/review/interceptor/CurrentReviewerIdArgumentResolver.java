package com.starrainnotes.review.interceptor;

import com.starrainnotes.account.api.CurrentActorApi;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/*
 * @CurrentReviewerId 的参数解析器。
 *
 * 只解析标注了该注解且类型为 Long 的参数；其他情况一律返回 false，
 * 让 Spring 继续用别的解析器，避免影响 Framework 原有行为。
 *
 * 未登录时 CurrentActorApi.current() 自己会抛 UNAUTHORIZED（401），
 * 这里不再包装，保证 401 的语义全局一致。
 */
@Component
public class CurrentReviewerIdArgumentResolver implements HandlerMethodArgumentResolver {

    private final CurrentActorApi currentActorApi;

    public CurrentReviewerIdArgumentResolver(CurrentActorApi currentActorApi) {
        this.currentActorApi = currentActorApi;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentReviewerId.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                  NativeWebRequest request, WebDataBinderFactory binderFactory) {
        return currentActorApi.current().getAccountId();
    }
}
