package com.starrainnotes.english.shared.events;

import org.springframework.jdbc.core.JdbcTemplate;

/** Slug and publish status for one table. The owning module supplies the table name. */
public final class JdbcContentStateSource implements EnglishContentStateSource {
    private final EnglishContentKind kind;
    private final JdbcTemplate jdbc;
    private final String sql;

    public JdbcContentStateSource(EnglishContentKind kind, JdbcTemplate jdbc, String table) {
        this.kind = kind;
        this.jdbc = jdbc;
        this.sql = "SELECT slug, publish_status FROM " + table + " WHERE id=?";
    }

    @Override
    public EnglishContentKind kind() {
        return kind;
    }

    @Override
    public EnglishContentState find(long id) {
        return jdbc.query(sql, rs -> rs.next()
                ? new EnglishContentState(rs.getString(1), "PUBLISHED".equals(rs.getString(2)))
                : null, id);
    }
}
