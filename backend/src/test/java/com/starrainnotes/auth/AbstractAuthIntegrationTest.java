package com.starrainnotes.auth;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared helpers for the frozen cookie+header CSRF flow:
 * fetch the XSRF-TOKEN cookie via GET /api/v1/auth/csrf, then attach both the
 * cookie and the X-XSRF-TOKEN header to state-changing requests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractAuthIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected String fetchCsrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        if (cookie == null) {
            throw new IllegalStateException("XSRF-TOKEN cookie missing from /api/v1/auth/csrf response");
        }
        return cookie.getValue();
    }

    protected MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("X-XSRF-TOKEN", token).cookie(new Cookie("XSRF-TOKEN", token));
    }

    protected MockHttpServletRequestBuilder jsonPost(String url, String body) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
