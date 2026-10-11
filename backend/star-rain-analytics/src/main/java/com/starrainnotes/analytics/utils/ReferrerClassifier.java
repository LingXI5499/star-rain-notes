/*
 * ReferrerClassifier：访问统计模块的工具文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 主要协作类型：ReferrerClassification。
 * 阅读入口：classify（classify处理入口）。
 */

package com.starrainnotes.analytics.utils;

import com.starrainnotes.analytics.dto.ReferrerClassification;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ReferrerClassifier，属于访问统计模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
@Component
public class ReferrerClassifier {
    /**
     * 搜索。
     */
    private static final Set<String> SEARCH = Set.of("google.com", "bing.com", "baidu.com", "sogou.com", "so.com");
    /**
     * SOCIAL。
     */
    private static final Set<String> SOCIAL = Set.of("weibo.com", "zhihu.com", "x.com", "twitter.com", "facebook.com", "linkedin.com");

    /**
     * frontend来源。
     */
    @Value("${star-rain.account.frontend-origin:}")
    private String frontendOrigin;

    /**
     * classify处理入口。
     * 关键处理步骤调用 <code>referrer.isBlank</code>、<code>referrer.length</code>、<code>URI.create</code>、<code>uri.getScheme</code>。
     * 边界判断包含 <code>(referrer == null || referrer.isBlank() || referrer.length() &gt; 2048)</code>；<code>(!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())))</code>。
     *
     * @param referrer referrer；类型为 <code>String</code>
     * @return 类型为 <code>ReferrerClassification</code> 的处理结果
     */
    public ReferrerClassification classify(String referrer) {
        if (referrer == null || referrer.isBlank() || referrer.length() > 2048) {
            return new ReferrerClassification("DIRECT", null);
        }
        try {
            URI uri = URI.create(referrer);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                return new ReferrerClassification("DIRECT", null);
            }
            String host = uri.getHost();
            if (host == null || host.length() > 255) {
                return new ReferrerClassification("DIRECT", null);
            }
            host = host.toLowerCase(Locale.ROOT);
            if (host.equals("localhost") || host.equals("127.0.0.1") || host.equals(configuredHost())) {
                return new ReferrerClassification("INTERNAL", host);
            }
            for (String domain : SEARCH) {
                if (host.equals(domain) || host.endsWith("." + domain)) return new ReferrerClassification("SEARCH", host);
            }
            for (String domain : SOCIAL) {
                if (host.equals(domain) || host.endsWith("." + domain)) return new ReferrerClassification("SOCIAL", host);
            }
            return new ReferrerClassification("OTHER", host);
        } catch (IllegalArgumentException ignored) {
            return new ReferrerClassification("DIRECT", null);
        }
    }

    /**
     * configured主机处理入口。
     * 关键处理步骤调用 <code>URI.create</code>、<code>host.toLowerCase</code>。
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String configuredHost() {
        try {
            String host = frontendOrigin == null ? null : URI.create(frontendOrigin).getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        }
        catch (IllegalArgumentException ignored) { return ""; }
    }
}
