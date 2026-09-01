package com.starrainnotes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the Spring context loads with the bootstrap configuration.
 */
@SpringBootTest
@ActiveProfiles("test")
class StarRainNotesApplicationTests {

    @Test
    void contextLoads() {
    }
}
