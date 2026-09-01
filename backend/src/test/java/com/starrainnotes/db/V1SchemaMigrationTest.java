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
 * Verifies that the public v1.0.0 baseline creates the final application
 * schema and only the documented public reference data on an empty database.
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
            "account_vocabulary_memory");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsAllFinalBusinessTables() {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                """, Integer.class);
        assertThat(count).isEqualTo(53);

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
    void flywayHistoryRecordsOneSuccessfulBaseline() {
        List<Long> successful = jdbc.queryForList("""
                SELECT success
                FROM flyway_schema_history
                WHERE version = '1'
                ORDER BY installed_rank
                """, Long.class);
        assertThat(successful).containsExactly(1L);

        List<String> descriptions = jdbc.queryForList("""
                SELECT description
                FROM flyway_schema_history
                ORDER BY installed_rank
                """, String.class);
        assertThat(descriptions).containsExactly("baseline");
    }

    @Test
    void seedsOnlyPublicReferenceData() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vocabulary_theme", Integer.class)).isEqualTo(60);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vocabulary_word", Integer.class)).isEqualTo(8_505);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM english_grammar_course", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM english_grammar_section", Integer.class)).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM english_grammar_lesson", Integer.class)).isEqualTo(42);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM english_taxonomy_term", Integer.class)).isEqualTo(49);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM english_cefr_standard", Integer.class)).isEqualTo(6);
        for (String table : List.of("admin_user", "user_account", "blog_post", "portfolio_project", "media_asset")) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isZero();
        }
    }
}
