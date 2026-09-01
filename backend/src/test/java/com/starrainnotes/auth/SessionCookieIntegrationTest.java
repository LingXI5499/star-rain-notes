package com.starrainnotes.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TASK-003 — end-to-end session flow against the real embedded Tomcat:
 * verifies the actual Set-Cookie attributes of the JSESSIONID session cookie
 * (HttpOnly, SameSite=Lax, Path=/, no Secure in dev) and that the SecurityContext
 * really persists across requests via the cookie jar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionCookieIntegrationTest extends AbstractAuthIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin-pass-1234";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedAdmin() {
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user (username, password_hash) VALUES (?, ?)",
                USERNAME, passwordEncoder.encode(PASSWORD));
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM admin_user");
    }

    @Test
    void realSessionFlowWithFrozenCookieAttributes() throws Exception {
        CookieManager cookieManager = new CookieManager();
        HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager).build();
        String base = "http://127.0.0.1:" + port;

        // anonymous session view
        HttpResponse<String> anonymous = send(client, base + "/api/v1/auth/session", null, null);
        assertThat(anonymous.statusCode()).isEqualTo(200);
        assertThat(anonymous.body()).contains("\"authenticated\":false");

        // CSRF cookie is issued
        send(client, base + "/api/v1/auth/csrf", null, null);
        String csrf = cookieValue(cookieManager, "XSRF-TOKEN");
        assertThat(csrf).isNotBlank();

        // login
        HttpResponse<String> login = send(client, base + "/api/v1/auth/login",
                "{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}", csrf);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains("\"authenticated\":true", "\"username\":\"" + USERNAME + "\"");

        // frozen session cookie attributes
        List<String> setCookies = login.headers().allValues("Set-Cookie");
        String sessionCookie = setCookies.stream()
                .filter(h -> h.startsWith("JSESSIONID"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("JSESSIONID cookie missing: " + setCookies));
        assertThat(sessionCookie)
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .contains("Path=/")
                .doesNotContain("Secure");

        String csrfCookie = setCookies.stream()
                .filter(h -> h.startsWith("XSRF-TOKEN"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("XSRF-TOKEN cookie missing: " + setCookies));
        assertThat(csrfCookie).contains("SameSite=Lax").doesNotContain("HttpOnly");

        // SecurityContext persists across a real next request
        HttpResponse<String> sessionAgain = send(client, base + "/api/v1/auth/session", null, null);
        assertThat(sessionAgain.body()).contains("\"authenticated\":true", "\"username\":\"" + USERNAME + "\"");

        // authenticated admin request passes the filter chain (200, not 401)
        HttpResponse<String> adminProbe = send(client, base + "/api/v1/admin/dashboard", null, null);
        assertThat(adminProbe.statusCode()).isEqualTo(200);

        // logout with the rotated CSRF token
        String csrfAfterLogin = cookieValue(cookieManager, "XSRF-TOKEN");
        HttpResponse<String> logout = sendPost(client, base + "/api/v1/auth/logout", csrfAfterLogin);
        assertThat(logout.statusCode()).isEqualTo(204);

        // session invalidated
        HttpResponse<String> afterLogout = send(client, base + "/api/v1/auth/session", null, null);
        assertThat(afterLogout.body()).contains("\"authenticated\":false");
    }

    private HttpResponse<String> send(HttpClient client, String url, String jsonBody, String csrfToken)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url));
        if (jsonBody != null) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            builder.GET();
        }
        if (csrfToken != null) {
            builder.header("X-XSRF-TOKEN", csrfToken);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPost(HttpClient client, String url, String csrfToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody());
        if (csrfToken != null) {
            builder.header("X-XSRF-TOKEN", csrfToken);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String cookieValue(CookieManager cookieManager, String name) {
        CookieStore store = cookieManager.getCookieStore();
        return store.getCookies().stream()
                .filter(c -> name.equals(c.getName()))
                .map(HttpCookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
