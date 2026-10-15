/*
 * EnglishRetirementMigrationTest：应用启动模块的测试文件。
 * 业务范围：多模块装配、入口路由、安全配置和启动初始化。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 阅读入口：freshInstallHasNoRetiredSchemaAndNeverClearsNewReadingOnRestart（freshInstall是否具备不含Retired数据库模式及NeverClearsNew阅读触发Restart处理入口）；upgradeAndOneOffResetPreserveOriginalWritingAndOtherEnglishData（upgrade及单次OffResetPreserve原始写作及Other英语Data处理入口）；assertRetiredSchemaAbsent（校验Retired数据库模式Absent）；flyway（flyway处理入口）。
 */

package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.apache.ibatis.session.SqlSession;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/** Only disposable schemas are migrated or cleared; never the configured application's schema. */
class EnglishRetirementMigrationTest extends MapperXmlIntegrationSupport {
    /**
     * 阅读。
     */
    private static final List<String> READING = List.of("revision", "alignment", "annotation",
            "vocabulary", "tag", "rights", "article");

    /**
     * freshInstall是否具备不含Retired数据库模式及NeverClearsNew阅读触发Restart处理入口。
     * 关键处理步骤调用 <code>inIsolatedDatabase</code>、<code>flyway</code>、<code>flyway.migrate</code>、<code>flyway.info</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    void freshInstallHasNoRetiredSchemaAndNeverClearsNewReadingOnRestart() throws Exception {
        inIsolatedDatabase((connection, schema) -> {
            Flyway flyway = flyway(connection, schema, null);
            flyway.migrate();
            assertEquals("2.047", flyway.info().current().getVersion().getVersion());
            assertRetiredSchemaAbsent(connection);
            try (Statement sql = connection.createStatement()) {
                sql.executeUpdate("INSERT INTO sr_english_reading_article(slug,title,summary,body_markdown,content_origin)"
                        + " VALUES('fresh-permanent','New English article','','English body.','ORIGINAL')");
                long before = count(connection, "sr_english_reading_article");
                flyway.migrate();
                assertEquals(before, count(connection, "sr_english_reading_article"));
            }
            flyway.validate();
        });
    }

    /**
     * upgrade及单次OffResetPreserve原始写作及Other英语Data处理入口。
     * 关键处理步骤调用 <code>inIsolatedDatabase</code>、<code>flyway</code>、<code>connection.createStatement</code>、<code>sql.executeUpdate</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    void upgradeAndOneOffResetPreserveOriginalWritingAndOtherEnglishData() throws Exception {
        inIsolatedDatabase((connection, schema) -> {
            flyway(connection, schema, "2.036").migrate();
            try (Statement sql = connection.createStatement()) {
                sql.executeUpdate("INSERT INTO sr_english_writing_article(owner_account_id,title,slug,body_markdown)"
                        + " VALUES(71,'Keep private','keep-private','Private original')");
                sql.executeUpdate("INSERT INTO sr_english_writing_revision(article_id,revision_no,snapshot_json,editor_account_id)"
                        + " SELECT id,1,JSON_OBJECT('body','Private original','owner',71),71"
                        + " FROM sr_english_writing_article WHERE slug='keep-private'");
            }
            long readings = count(connection, "sr_english_reading_article");
            long taxonomy = count(connection, "sr_english_taxonomy_term");
            long grammar = count(connection, "sr_english_grammar_lesson");
            long vocabulary = count(connection, "sr_english_vocabulary_word");
            try (Statement sql = connection.createStatement()) {
                sql.executeUpdate("UPDATE sr_english_overview SET introduction='An authored introduction',"
                        + " roadmap_markdown='An authored roadmap' WHERE id=1");
            }
            String writing = scalar(connection, "SELECT CONCAT(owner_account_id,':',visibility,':',body_markdown)"
                    + " FROM sr_english_writing_article WHERE slug='keep-private'");
            String revision = scalar(connection, "SELECT CAST(snapshot_json AS CHAR) FROM sr_english_writing_revision");
            String oldChecksums = scalar(connection, "SELECT GROUP_CONCAT(CONCAT(version,':',checksum) ORDER BY installed_rank)"
                    + " FROM flyway_schema_history WHERE success=1");
            Flyway upgraded = flyway(connection, schema, null);
            upgraded.migrate();
            assertRetiredSchemaAbsent(connection);
            assertEquals("An authored introduction", scalar(connection, "SELECT introduction FROM sr_english_overview WHERE id=1"));
            assertEquals("An authored roadmap", scalar(connection, "SELECT roadmap_markdown FROM sr_english_overview WHERE id=1"));
            assertEquals(readings, count(connection, "sr_english_reading_article"), "DDL must not clear readings");
            assertEquals(oldChecksums, scalar(connection, "SELECT GROUP_CONCAT(CONCAT(version,':',checksum) ORDER BY installed_rank)"
                    + " FROM flyway_schema_history WHERE success=1 AND version <= '2.036'"));
            // This is an explicit, isolated rehearsal, not a startup migration.
            connection.setAutoCommit(false);
            try (Statement sql = connection.createStatement()) {
                for (String suffix : READING) sql.executeUpdate("DELETE FROM sr_english_reading_" + suffix);
                for (String suffix : READING) assertEquals(0, count(connection, "sr_english_reading_" + suffix));
                connection.rollback();
                assertEquals(readings, count(connection, "sr_english_reading_article"), "default rollback protects data");
                for (String suffix : READING) sql.executeUpdate("DELETE FROM sr_english_reading_" + suffix);
                connection.commit();
                for (String suffix : READING) assertEquals(0, count(connection, "sr_english_reading_" + suffix));
                assertEquals(1, count(connection, "sr_english_writing_article"));
                assertEquals(1, count(connection, "sr_english_writing_revision"));
                assertEquals(writing, scalar(connection, "SELECT CONCAT(owner_account_id,':',visibility,':',body_markdown)"
                        + " FROM sr_english_writing_article WHERE slug='keep-private'"));
                assertEquals(revision, scalar(connection, "SELECT CAST(snapshot_json AS CHAR) FROM sr_english_writing_revision"));
                assertEquals(taxonomy, count(connection, "sr_english_taxonomy_term"));
                assertEquals(grammar, count(connection, "sr_english_grammar_lesson"));
                assertEquals(vocabulary, count(connection, "sr_english_vocabulary_word"));
                upgraded.migrate();
                assertEquals(0, count(connection, "sr_english_reading_article"));
                sql.executeUpdate("INSERT INTO sr_english_reading_article(slug,title,summary,body_markdown)"
                        + " VALUES('after-reset','New','','New English body')");
                connection.commit();
                upgraded.migrate();
                assertEquals(1, count(connection, "sr_english_reading_article"));
            }
            upgraded.validate();
        });
    }

    /**
     * 校验Retired数据库模式Absent。
     * 关键处理步骤调用 <code>scalar</code>。
     *
     * @param connection connection；类型为 <code>Connection</code>
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    private static void assertRetiredSchemaAbsent(Connection connection) throws Exception {
        assertEquals("0", scalar(connection, "SELECT COUNT(*) FROM information_schema.columns"
                + " WHERE table_schema=DATABASE() AND table_name='sr_english_reading_article'"
                + " AND column_name IN ('reading_level','cefr_level','level_assessed')"));
        assertEquals("0", scalar(connection, "SELECT COUNT(*) FROM information_schema.tables"
                + " WHERE table_schema=DATABASE() AND table_name IN ('sr_english_writing_resource','sr_english_writing_prompt')"));
    }

    /**
     * flyway处理入口。
     * 关键处理步骤调用 <code>Flyway.configure</code>、<code>config.target</code>、<code>config.load</code>。
     *
     * @param connection connection；类型为 <code>Connection</code>
     * @param schema 数据库模式；类型为 <code>String</code>
     * @param target 目标；类型为 <code>String</code>
     * @return 类型为 <code>Flyway</code> 的处理结果
     */
    private static Flyway flyway(Connection connection, String schema, String target) {
        var config = Flyway.configure().dataSource(new SingleConnectionDataSource(connection, true))
                .defaultSchema(schema).schemas(schema);
        if (target != null) config.target(target);
        return config.load();
    }

    /**
     * 统计英语RetirementMigration。
     * 关键处理步骤调用 <code>Long.parseLong</code>、<code>scalar</code>。
     *
     * @param connection connection；类型为 <code>Connection</code>
     * @param table table；类型为 <code>String</code>
     * @return 类型为 <code>long</code> 的处理结果
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    private static long count(Connection connection, String table) throws Exception {
        return Long.parseLong(scalar(connection, "SELECT COUNT(*) FROM " + table));
    }

    /**
     * scalar处理入口。
     * 关键处理步骤调用 <code>connection.createStatement</code>、<code>sql.executeQuery</code>、<code>row.next</code>、<code>row.getString</code>。
     *
     * @param connection connection；类型为 <code>Connection</code>
     * @param query 查询参数；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    private static String scalar(Connection connection, String query) throws Exception {
        try (Statement sql = connection.createStatement(); ResultSet row = sql.executeQuery(query)) {
            assertTrue(row.next());
            return row.getString(1);
        }
    }

    /**
     * 内部Isolated数据库处理入口。
     * 关键处理步骤调用 <code>openSession</code>、<code>session.getConnection</code>、<code>connection.getCatalog</code>、<code>Long.toUnsignedString</code>。
     *
     * @param check 检查；类型为 <code>DatabaseCheck</code>
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    private void inIsolatedDatabase(DatabaseCheck check) throws Exception {
        try (SqlSession session = openSession()) {
            Connection connection = session.getConnection();
            String original = connection.getCatalog();
            String schema = "sr_english_retirement_probe_" + Long.toUnsignedString(System.nanoTime());
            assertTrue(schema.matches("sr_english_retirement_probe_[0-9]+"));
            boolean created = false;
            try (Statement admin = connection.createStatement()) {
                try {
                    admin.execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
                    created = true;
                    connection.setCatalog(schema);
                    check.run(connection, schema);
                } finally {
                    connection.rollback();
                    connection.setCatalog(original);
                    if (created) admin.execute("DROP DATABASE " + schema);
                }
            }
        }
    }

    /**
     * 数据库检查，属于应用启动模块。
     * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
     */
    @FunctionalInterface
    private interface DatabaseCheck {
        /**
         * 运行数据库检查。
         *
         * @param connection connection；类型为 <code>Connection</code>
         * @param schema 数据库模式；类型为 <code>String</code>
         * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
         */
        void run(Connection connection, String schema) throws Exception;
    }
}
