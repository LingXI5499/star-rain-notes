/*
 * CurrentReviewerIdArgumentResolver：审核模块的模块支持文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：CurrentActorApi。
 * 阅读入口：supportsParameter（判断是否支持Parameter）；resolveArgument（解析Argument）。
 */

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
/**
 * 当前审核人标识ArgumentResolver，属于审核模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
public class CurrentReviewerIdArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * 提供当前认证操作者身份的跨模块接口。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi currentActorApi;

    /**
     * 构造CurrentReviewerIdArgumentResolver，接收本类使用的依赖或初始化参数。
     *
     * @param currentActorApi 提供当前认证操作者身份的跨模块接口；类型为 <code>CurrentActorApi</code>
     */
    public CurrentReviewerIdArgumentResolver(CurrentActorApi currentActorApi) {
        this.currentActorApi = currentActorApi;
    }

    /**
     * 判断是否支持Parameter。
     * 关键处理步骤调用 <code>parameter.hasParameterAnnotation</code>、<code>Long.class.equals</code>、<code>parameter.getParameterType</code>。
     *
     * @param parameter parameter；类型为 <code>MethodParameter</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentReviewerId.class)
                && Long.class.equals(parameter.getParameterType());
    }

    /**
     * 解析Argument。
     * 主要协作调用：<code>currentActorApi.current</code>。
     *
     * @param parameter parameter；类型为 <code>MethodParameter</code>
     * @param container container；类型为 <code>ModelAndViewContainer</code>
     * @param request 本次操作的请求参数对象；类型为 <code>NativeWebRequest</code>
     * @param binderFactory binderFactory；类型为 <code>WebDataBinderFactory</code>
     * @return 类型为 <code>Object</code> 的处理结果
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                  NativeWebRequest request, WebDataBinderFactory binderFactory) {
        return currentActorApi.current().getAccountId();
    }
}
