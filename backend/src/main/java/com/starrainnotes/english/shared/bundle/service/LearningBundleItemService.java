package com.starrainnotes.english.shared.bundle.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.shared.bundle.dto.BundleCatalogItemView;
import com.starrainnotes.english.shared.bundle.dto.BundleCatalogPageView;
import com.starrainnotes.english.shared.bundle.dto.BundleItemView;
import com.starrainnotes.english.shared.bundle.dto.BundleReadinessView;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class LearningBundleItemService {

    private static final List<String> CONTENT_TYPES = List.of("READING", "LISTENING", "WRITING");
    private static final List<String> PUBLISH_STATUSES = List.of("DRAFT", "PUBLISHED", "WITHDRAWN");
    private static final String CATALOG_SOURCE = """
            SELECT 'READING' content_type,a.id content_id,a.title,a.slug,a.summary,a.cefr_level,
                   m.public_url cover_url,a.publish_status,a.sort_order
            FROM english_reading_article a LEFT JOIN media_asset m ON m.id=a.cover_media_id
            UNION ALL
            SELECT 'LISTENING',a.id,a.title,a.slug,a.summary,a.cefr_level,
                   m.public_url,a.publish_status,a.sort_order
            FROM english_listening_item a LEFT JOIN media_asset m ON m.id=a.cover_media_id
            UNION ALL
            SELECT 'WRITING',a.id,a.title,a.slug,a.summary,a.cefr_level,
                   m.public_url,a.publish_status,a.sort_order
            FROM english_writing_prompt a LEFT JOIN media_asset m ON m.id=a.cover_media_id
            """;

    private final JdbcTemplate jdbc;

    public LearningBundleItemService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<BundleItemView> list(Long bundleId, boolean publishedOnly) {
        requireBundle(bundleId, publishedOnly);
        String status = publishedOnly ? " AND a.publish_status='PUBLISHED'" : "";
        List<BundleItemView> items = new ArrayList<>();
        items.addAll(jdbc.query("""
                SELECT 'READING' content_type,a.id content_id,a.title,a.slug,a.summary,a.cefr_level,
                       m.public_url cover_url,a.publish_status,x.sort_order
                FROM english_learning_bundle_reading_item x
                JOIN english_reading_article a ON a.id=x.article_id
                LEFT JOIN media_asset m ON m.id=a.cover_media_id
                WHERE x.bundle_id=?
                """ + status, this::mapItem, bundleId));
        items.addAll(jdbc.query("""
                SELECT 'LISTENING' content_type,a.id content_id,a.title,a.slug,a.summary,a.cefr_level,
                       m.public_url cover_url,a.publish_status,x.sort_order
                FROM english_learning_bundle_listening_item x
                JOIN english_listening_item a ON a.id=x.listening_item_id
                LEFT JOIN media_asset m ON m.id=a.cover_media_id
                WHERE x.bundle_id=?
                """ + status, this::mapItem, bundleId));
        items.addAll(jdbc.query("""
                SELECT 'WRITING' content_type,a.id content_id,a.title,a.slug,a.summary,a.cefr_level,
                       m.public_url cover_url,a.publish_status,x.sort_order
                FROM english_learning_bundle_writing_item x
                JOIN english_writing_prompt a ON a.id=x.prompt_id
                LEFT JOIN media_asset m ON m.id=a.cover_media_id
                WHERE x.bundle_id=?
                """ + status, this::mapItem, bundleId));
        items.sort(java.util.Comparator.comparingInt(BundleItemView::sortOrder)
                .thenComparing(BundleItemView::contentType)
                .thenComparing(BundleItemView::contentId));
        return items;
    }

    public BundleCatalogPageView catalog(Long bundleId, String rawType, String q, String rawStatus,
                                         String cefr, int page, int pageSize) {
        requireBundle(bundleId, false);
        String type = optionalType(rawType);
        String status = optionalStatus(rawStatus);
        int safePage = Math.max(1, page);
        int safePageSize = Math.min(50, Math.max(1, pageSize));

        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (type != null) {
            where.append(" AND c.content_type=?");
            args.add(type);
        }
        if (status != null) {
            where.append(" AND c.publish_status=?");
            args.add(status);
        }
        if (cefr != null && !cefr.isBlank()) {
            where.append(" AND c.cefr_level=?");
            args.add(cefr.trim().toUpperCase(Locale.ROOT));
        }
        if (q != null && !q.isBlank()) {
            where.append(" AND (LOWER(c.title) LIKE ? OR LOWER(c.slug) LIKE ? OR LOWER(c.summary) LIKE ?)");
            String term = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            args.add(term);
            args.add(term);
            args.add(term);
        }

        String from = " FROM (" + CATALOG_SOURCE + ") c" + where;
        Long totalValue = jdbc.queryForObject("SELECT COUNT(*)" + from, Long.class, args.toArray());
        long total = totalValue == null ? 0 : totalValue;
        int offset = (safePage - 1) * safePageSize;
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safePageSize);
        pageArgs.add(offset);

        Set<Key> selected = list(bundleId, false).stream()
                .map(item -> new Key(item.contentType(), item.contentId()))
                .collect(java.util.stream.Collectors.toSet());
        List<BundleCatalogItemView> items = jdbc.query(
                "SELECT c.*" + from + " ORDER BY FIELD(c.publish_status,'PUBLISHED','DRAFT','WITHDRAWN'),"
                        + " c.content_type,c.sort_order,c.content_id LIMIT ? OFFSET ?",
                (rs, row) -> new BundleCatalogItemView(
                        rs.getString("content_type"), rs.getLong("content_id"), rs.getString("title"),
                        rs.getString("slug"), rs.getString("summary"), rs.getString("cefr_level"),
                        rs.getString("cover_url"), rs.getString("publish_status"), rs.getInt("sort_order"),
                        selected.contains(new Key(rs.getString("content_type"), rs.getLong("content_id")))),
                pageArgs.toArray());
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safePageSize);
        return new BundleCatalogPageView(items, safePage, safePageSize, total, totalPages);
    }

    public BundleReadinessView readiness(Long bundleId) {
        requireBundle(bundleId, false);
        List<BundleItemView> items = list(bundleId, false);
        Map<String, Integer> counts = new LinkedHashMap<>();
        CONTENT_TYPES.forEach(type -> counts.put(type, 0));
        items.forEach(item -> counts.computeIfPresent(item.contentType(), (key, count) -> count + 1));
        int published = (int) items.stream().filter(item -> "PUBLISHED".equals(item.publishStatus())).count();
        int modules = (int) counts.values().stream().filter(count -> count > 0).count();
        Map<String, Object> metadata = jdbc.queryForMap(
                "SELECT summary,primary_cefr FROM english_learning_bundle WHERE id=?", bundleId);

        Set<String> issues = new LinkedHashSet<>();
        if (metadata.get("summary") == null || metadata.get("summary").toString().isBlank()) {
            issues.add("SUMMARY_REQUIRED");
        }
        if (metadata.get("primary_cefr") == null) {
            issues.add("CEFR_REQUIRED");
        }
        if (items.size() < 2) {
            issues.add("MINIMUM_ITEMS_REQUIRED");
        }
        if (modules < 2) {
            issues.add("MULTIPLE_MODULES_REQUIRED");
        }
        if (published != items.size()) {
            issues.add("UNPUBLISHED_ITEMS_PRESENT");
        }
        return new BundleReadinessView(items.size(), published, modules, counts,
                issues.isEmpty(), List.copyOf(issues));
    }

    public void assertPublishable(Long bundleId) {
        if (!readiness(bundleId).ready()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_BUNDLE_NOT_READY",
                    "Bundle is not ready", "Complete the bundle publishing checklist before publishing.");
        }
    }

    @Transactional
    public BundleItemView add(Long bundleId, String rawType, Long contentId) {
        requireMutableBundle(bundleId);
        String type = type(rawType);
        requireContent(type, contentId);
        int order = nextOrder(bundleId);
        try {
            jdbc.update("INSERT INTO " + table(type) + "(" + bundleColumn(type) + "," + idColumn(type)
                    + ",sort_order) VALUES (?,?,?)", bundleId, contentId, order);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "ENGLISH_BUNDLE_ITEM_DUPLICATE",
                    "Item already exists", "The content is already in this bundle.");
        }
        return list(bundleId, false).stream()
                .filter(item -> item.contentType().equals(type) && item.contentId().equals(contentId))
                .findFirst().orElseThrow();
    }

    @Transactional
    public void remove(Long bundleId, String rawType, Long contentId) {
        requireMutableBundle(bundleId);
        String type = type(rawType);
        int changed = jdbc.update("DELETE FROM " + table(type) + " WHERE " + bundleColumn(type)
                + "=? AND " + idColumn(type) + "=?", bundleId, contentId);
        if (changed == 0) notFound();
        normalize(bundleId);
    }

    @Transactional
    public void move(Long bundleId, String rawType, Long contentId, int target) {
        requireMutableBundle(bundleId);
        String type = type(rawType);
        List<Key> keys = list(bundleId, false).stream()
                .map(item -> new Key(item.contentType(), item.contentId()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Key selected = new Key(type, contentId);
        if (!keys.remove(selected)) notFound();
        keys.add(Math.min(Math.max(target, 0), keys.size()), selected);
        updateOrder(bundleId, keys);
    }

    public List<BundleItemView> publicList(String slug) {
        Long id;
        try {
            id = jdbc.queryForObject(
                    "SELECT id FROM english_learning_bundle WHERE slug=? AND publish_status='PUBLISHED'",
                    Long.class, slug);
        } catch (Exception ex) {
            throw unavailable();
        }
        if (!readiness(id).ready()) throw unavailable();
        return list(id, true);
    }

    private void normalize(Long bundleId) {
        List<Key> keys = list(bundleId, false).stream()
                .map(item -> new Key(item.contentType(), item.contentId())).toList();
        updateOrder(bundleId, keys);
    }

    private void updateOrder(Long bundleId, List<Key> keys) {
        for (int index = 0; index < keys.size(); index++) {
            Key key = keys.get(index);
            jdbc.update("UPDATE " + table(key.type()) + " SET sort_order=? WHERE " + bundleColumn(key.type())
                    + "=? AND " + idColumn(key.type()) + "=?", (index + 1) * 10, bundleId, key.id());
        }
    }

    private void requireBundle(Long id, boolean published) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM english_learning_bundle WHERE id=?"
                + (published ? " AND publish_status='PUBLISHED'" : ""), Integer.class, id);
        if (count == null || count == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_BUNDLE_NOT_FOUND",
                    "Bundle not found", "The learning bundle does not exist.");
        }
    }

    private void requireMutableBundle(Long id) {
        requireBundle(id, false);
        String status = jdbc.queryForObject(
                "SELECT publish_status FROM english_learning_bundle WHERE id=?", String.class, id);
        if ("PUBLISHED".equals(status)) {
            throw new ApiException(HttpStatus.CONFLICT, "ENGLISH_BUNDLE_PUBLISHED_LOCKED",
                    "Published bundle is locked", "Withdraw the bundle before changing its learning path.");
        }
    }

    private void requireContent(String type, Long id) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + contentTable(type) + " WHERE id=?", Integer.class, id);
        if (count == null || count == 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_BUNDLE_ITEM_INVALID",
                    "Invalid bundle item", "The selected content does not exist.");
        }
    }

    private int nextOrder(Long bundleId) {
        return list(bundleId, false).stream().mapToInt(BundleItemView::sortOrder).max().orElse(0) + 10;
    }

    private String optionalType(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw)) return null;
        return type(raw);
    }

    private String optionalStatus(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw)) return null;
        String status = raw.toUpperCase(Locale.ROOT);
        if (!PUBLISH_STATUSES.contains(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ENGLISH_BUNDLE_CATALOG_FILTER_INVALID",
                    "Invalid catalog filter", "The selected publishing status is invalid.");
        }
        return status;
    }

    private String type(String raw) {
        String type = raw == null ? "" : raw.toUpperCase(Locale.ROOT);
        if (!CONTENT_TYPES.contains(type)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_BUNDLE_ITEM_INVALID",
                    "Invalid bundle item", "Only reading, listening and writing content can be added.");
        }
        return type;
    }

    private String table(String type) {
        return switch (type) {
            case "READING" -> "english_learning_bundle_reading_item";
            case "LISTENING" -> "english_learning_bundle_listening_item";
            default -> "english_learning_bundle_writing_item";
        };
    }

    private String contentTable(String type) {
        return switch (type) {
            case "READING" -> "english_reading_article";
            case "LISTENING" -> "english_listening_item";
            default -> "english_writing_prompt";
        };
    }

    private String bundleColumn(String type) {
        return "bundle_id";
    }

    private String idColumn(String type) {
        return switch (type) {
            case "READING" -> "article_id";
            case "LISTENING" -> "listening_item_id";
            default -> "prompt_id";
        };
    }

    private BundleItemView mapItem(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new BundleItemView(rs.getString("content_type"), rs.getLong("content_id"),
                rs.getString("title"), rs.getString("slug"), rs.getString("summary"),
                rs.getString("cefr_level"), rs.getString("cover_url"), rs.getString("publish_status"),
                rs.getInt("sort_order"));
    }

    private void notFound() {
        throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_BUNDLE_ITEM_NOT_FOUND",
                "Bundle item not found", "The selected bundle item does not exist.");
    }

    private ApiException unavailable() {
        return new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_CONTENT_NOT_PUBLISHED",
                "Bundle unavailable", "The learning bundle is not ready for public access.");
    }

    private record Key(String type, Long id) {
    }
}
