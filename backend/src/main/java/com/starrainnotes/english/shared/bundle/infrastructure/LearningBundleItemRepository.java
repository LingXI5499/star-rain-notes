package com.starrainnotes.english.shared.bundle.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/** Persistence of bundle metadata and membership only. */
@Repository
public class LearningBundleItemRepository {
    private final JdbcTemplate jdbc;

    public LearningBundleItemRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Member(String type, long contentId, int sortOrder) { }
    public record Metadata(String summary, String primaryCefr) { }

    public List<Member> members(Long bundleId) {
        List<Member> members = new ArrayList<>();
        members.addAll(jdbc.query("SELECT article_id,sort_order FROM english_learning_bundle_reading_item WHERE bundle_id=?",
                (rs, row) -> new Member("READING", rs.getLong(1), rs.getInt(2)), bundleId));
        members.addAll(jdbc.query("SELECT listening_item_id,sort_order FROM english_learning_bundle_listening_item WHERE bundle_id=?",
                (rs, row) -> new Member("LISTENING", rs.getLong(1), rs.getInt(2)), bundleId));
        members.addAll(jdbc.query("SELECT prompt_id,sort_order FROM english_learning_bundle_writing_item WHERE bundle_id=?",
                (rs, row) -> new Member("WRITING", rs.getLong(1), rs.getInt(2)), bundleId));
        members.sort(java.util.Comparator.comparingInt(Member::sortOrder)
                .thenComparing(Member::type).thenComparingLong(Member::contentId));
        return members;
    }

    public Metadata metadata(Long bundleId) {
        return jdbc.queryForObject("SELECT summary,primary_cefr FROM english_learning_bundle WHERE id=?",
                (rs, row) -> new Metadata(rs.getString(1), rs.getString(2)), bundleId);
    }

    public boolean exists(Long id, boolean publishedOnly) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM english_learning_bundle WHERE id=?"
                + (publishedOnly ? " AND publish_status='PUBLISHED'" : ""), Integer.class, id);
        return count != null && count > 0;
    }

    public String publishStatus(Long id) {
        return jdbc.queryForObject("SELECT publish_status FROM english_learning_bundle WHERE id=?", String.class, id);
    }

    public Long publishedIdBySlug(String slug) {
        return jdbc.queryForObject("SELECT id FROM english_learning_bundle WHERE slug=? AND publish_status='PUBLISHED'",
                Long.class, slug);
    }

    public void add(Long bundleId, String type, Long contentId, int sortOrder) {
        jdbc.update("INSERT INTO " + table(type) + "(bundle_id," + idColumn(type)
                + ",sort_order) VALUES (?,?,?)", bundleId, contentId, sortOrder);
    }

    public int remove(Long bundleId, String type, Long contentId) {
        return jdbc.update("DELETE FROM " + table(type) + " WHERE bundle_id=? AND " + idColumn(type) + "=?",
                bundleId, contentId);
    }

    public void updateOrder(Long bundleId, String type, Long contentId, int sortOrder) {
        jdbc.update("UPDATE " + table(type) + " SET sort_order=? WHERE bundle_id=? AND " + idColumn(type) + "=?",
                sortOrder, bundleId, contentId);
    }

    private String table(String type) {
        return switch (type) {
            case "READING" -> "english_learning_bundle_reading_item";
            case "LISTENING" -> "english_learning_bundle_listening_item";
            case "WRITING" -> "english_learning_bundle_writing_item";
            default -> throw new IllegalArgumentException("Unsupported bundle content type: " + type);
        };
    }

    private String idColumn(String type) {
        return switch (type) {
            case "READING" -> "article_id";
            case "LISTENING" -> "listening_item_id";
            case "WRITING" -> "prompt_id";
            default -> throw new IllegalArgumentException("Unsupported bundle content type: " + type);
        };
    }
}
