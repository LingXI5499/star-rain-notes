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

class ApiNotFoundContractTest {
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

    @Configuration
    @EnableWebMvc
    static class MvcConfig implements WebMvcConfigurer {
        @Bean
        GlobalApiExceptionHandler apiErrors() {
            return new GlobalApiExceptionHandler();
        }

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/api/**").addResourceLocations("classpath:/absent-test-resources/");
        }
    }
}
