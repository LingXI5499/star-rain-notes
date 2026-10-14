package com.starrainnotes.common.web;

import com.starrainnotes.common.exception.ApiException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** HTTP editing precondition, evaluated while the owning aggregate row is locked. */
public final class ContentVersion {
    private static final String VERSION = ContentVersion.class.getName() + ".version";
    private ContentVersion() { }

    public static void require(Long version) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes context)) return;
        var request = context.getRequest();
        if ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod())) return;
        if ("POST".equals(request.getMethod()) && request.getRequestURI().matches(".*/admin/(blog/posts|portfolio/works)$")) return;
        String header = request.getHeader("If-Match");
        if (header == null || header.isBlank()) {
            throw new ApiException("CONTENT_VERSION_REQUIRED", "请重新打开编辑页面后再提交修改。", 428);
        }
        long expected;
        try { expected = Long.parseLong(header.replace("\"", "")); }
        catch (NumberFormatException invalid) {
            throw new ApiException("CONTENT_VERSION_INVALID", "内容版本格式无效。", 400);
        }
        long current = version == null ? 0 : version;
        if (expected != current) {
            throw new ApiException("CONTENT_VERSION_CONFLICT", "内容已被其他窗口修改。当前输入已保留，请核对最新版本后再保存。", 409);
        }
        request.setAttribute(VERSION, current);
    }

    public static void advanced() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes context
                && context.getRequest().getAttribute(VERSION) instanceof Long version) {
            context.getRequest().setAttribute(VERSION, version + 1);
        }
    }

    public static Long committedVersion() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes context
                && context.getRequest().getAttribute(VERSION) instanceof Long version) return version;
        return null;
    }
}
