package com.starrainnotes.site.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SiteSeo implements SiteSeoPort {
    private final JdbcTemplate jdbc;

    public SiteSeo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Identity identity() {
        return jdbc.query("SELECT site_name,tagline,default_seo_description,github_url FROM site_setting WHERE id=1",
                rs -> rs.next() ? new Identity(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)) : null);
    }
}
