/*
 * ReviewWebConfig：审核模块的装配配置文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 * 主要协作类型：CurrentReviewerIdArgumentResolver。
 * 阅读入口：addArgumentResolvers（添加ArgumentResolvers）。
 */

package com.starrainnotes.review.config;

import com.starrainnotes.review.interceptor.CurrentReviewerIdArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
 * Review 模块的 Web 层配置。
 *
 * 只注册自己的参数解析器。刻意不在这里写 @MapperScan（那是 ReviewPersistenceConfig 的职责），
 * 也不在这里加 CORS / 拦截器——跨模块的 Web 配置属于 boot 或 Account 的唯一安全链。
 */
/**
 * 审核Web配置，属于审核模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Configuration
public class ReviewWebConfig implements WebMvcConfigurer {

    /**
     * 当前审核人标识ArgumentResolver。
     */
    private final CurrentReviewerIdArgumentResolver currentReviewerIdArgumentResolver;

    /**
     * 构造ReviewWebConfig，接收本类使用的依赖或初始化参数。
     *
     * @param currentReviewerIdArgumentResolver 当前审核人标识ArgumentResolver；类型为 <code>CurrentReviewerIdArgumentResolver</code>
     */
    public ReviewWebConfig(CurrentReviewerIdArgumentResolver currentReviewerIdArgumentResolver) {
        this.currentReviewerIdArgumentResolver = currentReviewerIdArgumentResolver;
    }

    /**
     * 添加ArgumentResolvers。
     * 关键处理步骤调用 <code>resolvers.add</code>。
     *
     * @param resolvers resolvers；类型为 <code>List&lt;HandlerMethodArgumentResolver&gt;</code>
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentReviewerIdArgumentResolver);
    }
}
