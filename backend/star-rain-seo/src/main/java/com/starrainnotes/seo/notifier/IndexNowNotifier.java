/*
 * IndexNowNotifier：公开页面与 SEO模块的模块支持文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：NotificationResult、SeoChange、SeoProperties、CanonicalUrlResolver。
 * 阅读入口：providerCode（提供方代码处理入口）；enabled（已启用处理入口）；notify（通知索引NowNotifier）。
 */

package com.starrainnotes.seo.notifier;

import com.starrainnotes.seo.dto.NotificationResult;

import com.starrainnotes.seo.dto.SeoChange;

import com.starrainnotes.seo.config.SeoProperties;
import com.starrainnotes.seo.config.CanonicalUrlResolver;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * 索引NowNotifier，属于公开页面与 SEO模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class IndexNowNotifier {
    /**
     * 本模块的外部配置属性。
     * 协作类型：<code>SeoProperties</code>。
     */
    private final SeoProperties properties;
    /**
     * 规范。
     */
    private final CanonicalUrlResolver canonical;

    /**
     * 提供方代码处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String providerCode() { return "INDEXNOW"; }

    /**
     * 已启用处理入口。
     * 主要协作调用：<code>properties.getIndexnow</code>。
     * @return 本方法条件是否成立的判断结果
     */
    public boolean enabled() {
        var config = properties.getIndexnow();
        return config != null && config.isEnabled() && config.getKey() != null
            && !config.getKey().isBlank() && config.getEndpoint() != null && !config.getEndpoint().isBlank();
    }

    /**
     * 通知索引NowNotifier。
     * 主要协作调用：<code>properties.getIndexnow</code>、<code>canonical.baseUrl</code>。
     *
     * @param change change；类型为 <code>SeoChange</code>
     * @return 类型为 <code>NotificationResult</code> 的处理结果
     */
    public NotificationResult notify(SeoChange change) {
        if (!enabled()) return NotificationResult.builder().errorCode("PROVIDER_DISABLED").build();
        URI endpoint = URI.create(properties.getIndexnow().getEndpoint());
        if (!"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null) {
            return NotificationResult.builder().errorCode("INVALID_ENDPOINT").build();
        }
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofSeconds(5));
            factory.setReadTimeout(Duration.ofSeconds(10));
            int status = RestClient.builder().requestFactory(factory).build().post().uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("host", URI.create(canonical.baseUrl()).getHost(),
                    "key", properties.getIndexnow().getKey(),
                    "keyLocation", canonical.canonical("/api/public/seo/indexnow-key.txt"),
                    "urlList", List.of(change.getCanonicalUrl())))
                .retrieve().toBodilessEntity().getStatusCode().value();
            return NotificationResult.builder().success(status >= 200 && status < 300).httpStatus(status).build();
        } catch (RestClientResponseException response) {
            return NotificationResult.builder().httpStatus(response.getStatusCode().value())
                .errorCode("HTTP_" + response.getStatusCode().value()).build();
        } catch (RuntimeException exception) {
            return NotificationResult.builder().errorCode("TRANSPORT_ERROR").build();
        }
    }
}
