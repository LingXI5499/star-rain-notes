package com.starrainnotes.seo.notify;

import com.starrainnotes.seo.canonical.CanonicalService;
import com.starrainnotes.seo.config.SeoProperties;
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

@Component
@RequiredArgsConstructor
public class IndexNowNotifier implements SearchEngineNotifier {
    private final SeoProperties properties;
    private final CanonicalService canonical;

    @Override public String providerCode() { return "INDEXNOW"; }

    @Override
    public boolean enabled() {
        var config = properties.getIndexnow();
        return config != null && config.isEnabled() && config.getKey() != null
            && !config.getKey().isBlank() && config.getEndpoint() != null && !config.getEndpoint().isBlank();
    }

    @Override
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
