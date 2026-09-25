package com.starrainnotes.profile.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProfileSeo implements ProfileSeoPort {
    private final JdbcTemplate jdbc;

    public ProfileSeo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String displayName() {
        return jdbc.query("SELECT display_name FROM profile WHERE id=1", rs -> rs.next() ? rs.getString(1) : null);
    }

    @Override
    public About about() {
        return jdbc.query("""
                SELECT display_name,headline,bio,github_url,technical_direction_markdown,journey_markdown,updated_at
                FROM profile WHERE id=1
                """, rs -> rs.next() ? new About(rs.getString("display_name"), rs.getString("headline"), rs.getString("bio"),
                rs.getString("github_url"), rs.getString("technical_direction_markdown"), rs.getString("journey_markdown"),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime()) : null);
    }
}
