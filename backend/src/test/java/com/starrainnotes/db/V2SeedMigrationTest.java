package com.starrainnotes.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TASK-002 — verifies the frozen singleton seed (03-database-design.md §9):
 * site_setting id=1, profile id=1, english_overview id=1.
 * admin_user MUST be empty (no default admin).
 */
@SpringBootTest
@ActiveProfiles("test")
class V2SeedMigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void siteSettingSingletonIsSeeded() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM site_setting", Integer.class);
        assertThat(count).isEqualTo(1);

        String siteName = jdbc.queryForObject("SELECT site_name FROM site_setting WHERE id = 1", String.class);
        String tagline = jdbc.queryForObject("SELECT tagline FROM site_setting WHERE id = 1", String.class);
        String timezone = jdbc.queryForObject("SELECT timezone FROM site_setting WHERE id = 1", String.class);
        String seo = jdbc.queryForObject("SELECT default_seo_description FROM site_setting WHERE id = 1", String.class);

        assertThat(siteName).isEqualTo("星雨笔录");
        assertThat(tagline).isEqualTo("Knowledge · Code · Growth");
        assertThat(timezone).isEqualTo("Asia/Shanghai");
        assertThat(seo).isNotEmpty();
    }

    @Test
    void profileSingletonIsSeededWithValidJsonFocus() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM profile", Integer.class);
        assertThat(count).isEqualTo(1);

        Integer validJson = jdbc.queryForObject(
                "SELECT JSON_VALID(current_focus) FROM profile WHERE id = 1", Integer.class);
        assertThat(validJson).isEqualTo(1);
    }

    @Test
    void englishOverviewSingletonIsSeeded() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM english_overview", Integer.class);
        assertThat(count).isEqualTo(1);

        String title = jdbc.queryForObject("SELECT title FROM english_overview WHERE id = 1", String.class);
        String stage = jdbc.queryForObject("SELECT current_stage FROM english_overview WHERE id = 1", String.class);
        assertThat(title).isEqualTo("English");
        assertThat(stage).isEqualTo("FOUNDATION");
    }

    @Test
    void adminUserIsEmptyOnFreshInstall() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM admin_user", Integer.class);
        assertThat(count).isZero();
    }
}
