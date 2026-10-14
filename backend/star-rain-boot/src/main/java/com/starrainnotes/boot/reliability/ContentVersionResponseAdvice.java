package com.starrainnotes.boot.reliability;

import com.starrainnotes.common.web.ContentVersion;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class ContentVersionResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override public boolean supports(MethodParameter type, Class<? extends HttpMessageConverter<?>> converter) { return true; }
    @Override public Object beforeBodyWrite(Object body, MethodParameter type, MediaType mediaType,
            Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request, ServerHttpResponse response) {
        Long version = ContentVersion.committedVersion();
        if (version != null && response instanceof ServletServerHttpResponse servlet
                && servlet.getServletResponse().getStatus() >= 200 && servlet.getServletResponse().getStatus() < 300) {
            response.getHeaders().set("X-Content-Version", String.valueOf(version));
        }
        return body;
    }
}
