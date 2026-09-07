package com.starrainnotes.common.web;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Anonymous public GET APIs should advertise a short shared cache window.
 */
class PublicApiCacheHeadersFilterTest extends AbstractAuthIntegrationTest {

    @Test
    void publicGetResponsesIncludeShortCacheControl() throws Exception {
        mockMvc.perform(get("/api/v1/public/site"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "public, max-age=30, stale-while-revalidate=60"));
    }

    @Test
    void adminApisAreNotMarkedPubliclyCacheable() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("stale-while-revalidate"))));
    }
}
