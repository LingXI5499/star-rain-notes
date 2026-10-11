/*
 * ApiNotFoundContractTest：应用启动模块的测试文件。
 * 业务范围：多模块装配、入口路由、安全配置和启动初始化。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：GlobalApiExceptionHandler。
 * 阅读入口：addResourceHandlers（添加ResourceHandlers）。
 */

package com.starrainnotes.boot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.starrainnotes.common.handler.GlobalApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 接口不存在Contract测试，属于应用启动模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class ApiNotFoundContractTest {
    /**
     * 缺失Mvc及StaticResource路由集合Return不存在Instead所属ServerErrors处理入口。
     * 关键处理步骤调用 <code>context.setServletContext</code>、<code>context.register</code>、<code>context.refresh</code>、<code>MockMvcBuilders.webAppContextSetup</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    void missingMvcAndStaticResourceRoutesReturnNotFoundInsteadOfServerErrors() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(MvcConfig.class);
            context.refresh();
            var mvc = MockMvcBuilders.webAppContextSetup(context).build();
            for (String path : new String[]{"/api/public/english/content/writing-resources",
                    "/api/admin/english/content/writing-prompts", "/unregistered-mvc-route"}) {
                mvc.perform(get(path)).andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
            }
        }
    }

    /**
     * Mvc配置，属于应用启动模块。
     * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
     */
    @Configuration
    @EnableWebMvc
    static class MvcConfig implements WebMvcConfigurer {
        /**
         * 接口Errors处理入口。
         * @return 类型为 <code>GlobalApiExceptionHandler</code> 的处理结果
         */
        @Bean
        GlobalApiExceptionHandler apiErrors() {
            return new GlobalApiExceptionHandler();
        }

        /**
         * 添加ResourceHandlers。
         * 关键处理步骤调用 <code>registry.addResourceHandler</code>。
         *
         * @param registry registry；类型为 <code>ResourceHandlerRegistry</code>
         */
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/api/**").addResourceLocations("classpath:/absent-test-resources/");
        }
    }
}
