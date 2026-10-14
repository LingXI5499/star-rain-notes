package com.starrainnotes.boot.reliability;

import com.starrainnotes.common.result.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContentHistoryController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public ContentHistoryController(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }
    @GetMapping("/api/admin/blog/posts/{id}/revisions")
    @PreAuthorize("hasAuthority('blog:read-admin')")
    public ApiResponse<List<Revision>> blog(@PathVariable Long id) { return history("BLOG", id); }
    @GetMapping("/api/admin/portfolio/works/{id}/revisions")
    @PreAuthorize("hasAuthority('portfolio:read-admin')")
    public ApiResponse<List<Revision>> work(@PathVariable Long id) { return history("PORTFOLIO", id); }
    @GetMapping("/api/admin/blog/posts/{id}/revisions/{version}")
    @PreAuthorize("hasAuthority('blog:read-admin')")
    public ApiResponse<Revision> blogVersion(@PathVariable Long id, @PathVariable long version) { return revision("BLOG", id, version); }
    @GetMapping("/api/admin/portfolio/works/{id}/revisions/{version}")
    @PreAuthorize("hasAuthority('portfolio:read-admin')")
    public ApiResponse<Revision> workVersion(@PathVariable Long id, @PathVariable long version) { return revision("PORTFOLIO", id, version); }

    private ApiResponse<List<Revision>> history(String type, Long id) {
        return ApiResponse.ok(jdbc.query("SELECT row_version,JSON_OBJECT('title',JSON_UNQUOTE(JSON_EXTRACT(snapshot_json,'$.title'))),created_at FROM sr_content_revision "
                + "WHERE content_type=? AND content_id=? ORDER BY row_version DESC LIMIT 100", (row, index) -> {
            try { return new Revision(row.getLong(1), json.readTree(row.getString(2)), row.getTimestamp(3).toInstant().toString()); }
            catch (java.io.IOException invalid) { throw new IllegalStateException("Invalid content history", invalid); }
        }, type, id));
    }
    private ApiResponse<Revision> revision(String type, Long id, long version) {
        var rows = jdbc.query("SELECT row_version,snapshot_json,created_at FROM sr_content_revision WHERE content_type=? AND content_id=? AND row_version=?", (row, index) -> {
            try { return new Revision(row.getLong(1), json.readTree(row.getString(2)), row.getTimestamp(3).toInstant().toString()); }
            catch (java.io.IOException invalid) { throw new IllegalStateException("Invalid content history", invalid); }
        }, type, id, version);
        if (rows.isEmpty()) throw new com.starrainnotes.common.exception.ApiException("CONTENT_REVISION_NOT_FOUND", "历史版本不存在或已超出保留范围。", 404);
        return ApiResponse.ok(rows.getFirst());
    }
    public record Revision(long rowVersion, JsonNode snapshot, String createdAt) { }
}
