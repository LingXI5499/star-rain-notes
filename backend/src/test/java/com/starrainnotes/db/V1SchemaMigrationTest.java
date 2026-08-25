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
            "english_grammar_course",
            "english_grammar_section",
            "english_grammar_lesson",
            "english_taxonomy_term",
            "english_cefr_standard",
            "english_exercise",
            "english_learning_bundle",
            "vocabulary_word_family",
            "vocabulary_family_member",
            "vocabulary_word_family_link",
            "english_reading_article",
            "english_reading_article_tag",
            "english_reading_article_exercise",
            "english_learning_bundle_reading_item",
            "english_reading_article_grammar_lesson",
            "english_listening_item",
            "english_listening_segment",
            "english_listening_item_tag",
            "english_listening_item_exercise",
            "english_learning_bundle_listening_item",
            "english_reading_listening_pair",
            "english_listening_pronunciation_rule",
            "english_writing_resource",
            "english_writing_resource_tag",
            "english_writing_prompt",
            "english_writing_prompt_tag",
            "english_writing_prompt_exercise",
            "english_learning_bundle_writing_item",
            "english_learner_profile",
            "english_learning_record",
            "english_learning_attempt",
            "english_writing_submission",
            "user_account",
            "admin_invitation",
            "email_verification_challenge",
            "admin_audit_log",
            "content_review_request",
            "tutorial_node_hierarchy_backup_v5",
            "tutorial_category_full_backup_v7",
            "tutorial_full_backup_v7",
            "tutorial_node_full_backup_v7",
            "tutorial_authored_chapter_backup_v7");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsAllCoreAndBackupTables() {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                """, Integer.class);
        assertThat(count).isEqualTo(57);

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
    void flywayHistoryRecordsV1ThroughV20AsSuccessful() {
        List<Long> successful = jdbc.queryForList("""
                SELECT success
                FROM flyway_schema_history
                WHERE version IN ('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20')
                ORDER BY installed_rank
                """, Long.class);
        assertThat(successful).containsExactly(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L);

        List<String> descriptions = jdbc.queryForList("""
                SELECT description
                FROM flyway_schema_history
                ORDER BY installed_rank
                """, String.class);
        assertThat(descriptions).containsExactly(
                "init schema", "seed system singletons", "create vocabulary", "seed vocabulary",
                "flatten tutorial hierarchy", "enforce curriculum parents",
                "restore authoritative tutorial taxonomy", "create english grammar",
                "create english shared foundation", "create english reading", "harden english reading",
                "create english listening", "harden english listening", "create english writing",
                "create english learning loop", "create english learning insights",
                "add english learning analytics indexes", "create account collaboration",
                "create admin audit log", "create content review request");
    }
}
