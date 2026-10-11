package com.starrainnotes.portfolio.utils;

import com.starrainnotes.portfolio.exception.WorkInvalidException;
import java.net.URI;

public final class WorkLinkRules {
    private WorkLinkRules() {
    }

    public static String requireSafeUrl(String input) {
        if (input == null || input.length() > 1000) {
            throw new WorkInvalidException("外部链接长度无效");
        }
        try {
            URI uri = URI.create(input.trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new WorkInvalidException("外部链接只接受 http/https 地址");
            }
            return uri.toString();
        } catch (IllegalArgumentException exception) {
            throw new WorkInvalidException("外部链接地址无效");
        }
    }
}
