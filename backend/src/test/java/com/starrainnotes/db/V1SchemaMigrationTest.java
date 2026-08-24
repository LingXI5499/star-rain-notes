package com.starrainnotes.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TASK-002 (+ approved vocabulary spec change) — verifies Flyway executes the
 * frozen schema on a fresh database: the core business tables plus the V5
 * hierarchy and authoritative-taxonomy backups, with the frozen charset/collation and every
 * migration recorded as successful.
 */
@SpringBootTest
@ActiveProfiles("test")
class V1SchemaMigrationTest {

    private static final List<String> CORE_TABLES = List.of(
            "admin_user",
            "media_asset",
            "site_setting",
            "tutorial_category",
            "tutorial",
            "tutorial_node",
            "blog_post",
            "blog_tag",
            "blog_post_tag",
            "portfolio_project",
            "profile",
            "profile_selected_content",
            "english_overview",
            "vocabulary_theme",
            "vocabulary_word",
            "tutorial_node_hierarchy_backup_v5",
            "tutorial_category_full_backup_v7",
            "tutorial_full_backup_v7",
            "tutorial_node_full_backup_v7",
            "tutorial_authored_chapter_backup_v7");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsAllTwentyCoreAndBackupTables() {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                """, Integer.class);
        assertThat(count).isEqualTo(20);

        List<String> names = jdbc.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                """, String.class);
        assertThat(Set.copyOf(names)).containsExactlyInAnyOrderElementsOf(CORE_TABLES);
    }

    @Test
    void allCoreTablesUseFrozenCharsetAndCollation() {
        List<String> badCollations = jdbc.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                  AND table_collation <> 'utf8mb4_0900_ai_ci'
                """, String.class);
        assertThat(badCollations).isEmpty();
    }

    @Test
    void flywayHistoryRecordsV1ThroughV7AsSuccessful() {
        List<Long> successful = jdbc.queryForList("""
                SELECT success
                FROM flyway_schema_history
                WHERE version IN ('1', '2', '3', '4', '5', '6', '7')
                ORDER BY installed_rank
                """, Long.class);
        assertThat(successful).containsExactly(1L, 1L, 1L, 1L, 1L, 1L, 1L);

        List<String> descriptions = jdbc.queryForList("""
                SELECT description
                FROM flyway_schema_history
                ORDER BY installed_rank
                """, String.class);
        assertThat(descriptions).containsExactly(
                "init schema", "seed system singletons", "create vocabulary", "seed vocabulary",
                "flatten tutorial hierarchy", "enforce curriculum parents",
                "restore authoritative tutorial taxonomy");
    }
}
