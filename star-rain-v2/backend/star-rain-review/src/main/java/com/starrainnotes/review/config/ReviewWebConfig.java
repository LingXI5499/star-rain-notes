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
@Configuration
public class ReviewWebConfig implements WebMvcConfigurer {

    private final CurrentReviewerIdArgumentResolver currentReviewerIdArgumentResolver;

    public ReviewWebConfig(CurrentReviewerIdArgumentResolver currentReviewerIdArgumentResolver) {
        this.currentReviewerIdArgumentResolver = currentReviewerIdArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentReviewerIdArgumentResolver);
    }
}
