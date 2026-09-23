package com.starrainnotes.english.shared.bundle.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.shared.bundle.dto.BundleCatalogItemView;
import com.starrainnotes.english.shared.bundle.dto.BundleCatalogPageView;
import com.starrainnotes.english.shared.bundle.dto.BundleItemView;
import com.starrainnotes.english.shared.bundle.dto.BundleReadinessView;
import com.starrainnotes.english.shared.bundle.infrastructure.LearningBundleItemRepository;
import com.starrainnotes.english.shared.content.ContentCatalogFilter;
import com.starrainnotes.english.shared.content.ContentCatalogSlice;
import com.starrainnotes.english.shared.content.ContentDescriptor;
import com.starrainnotes.english.shared.content.EnglishContentRegistry;
import com.starrainnotes.english.shared.content.EnglishContentType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LearningBundleItemService {
    private static final List<String> CONTENT_TYPES = List.of("READING", "LISTENING", "WRITING");
    private static final List<String> PUBLISH_STATUSES = List.of("DRAFT", "PUBLISHED", "WITHDRAWN");

    private final LearningBundleItemRepository repository;
    private final EnglishContentRegistry content;

    public LearningBundleItemService(LearningBundleItemRepository repository, EnglishContentRegistry content) {
        this.repository = repository;
        this.content = content;
    }

    public List<BundleItemView> list(Long bundleId, boolean publishedOnly) {
        requireBundle(bundleId, publishedOnly);
        List<BundleItemView> items = new ArrayList<>();
        for (LearningBundleItemRepository.Member member : repository.members(bundleId)) {
            ContentDescriptor descriptor = content.require(EnglishContentType.valueOf(member.type()), member.contentId());
            if (!publishedOnly || descriptor.published()) {
                items.add(toItem(descriptor, member.sortOrder()));
            }
        }
        return items;
    }

    public BundleCatalogPageView catalog(Long bundleId, String rawType, String q, String rawStatus,
                                         String cefr, int page, int pageSize) {
        requireBundle(bundleId, false);
        String type = optionalType(rawType);
        String status = optionalStatus(rawStatus);
        String level = cefr == null || cefr.isBlank() ? null : cefr.trim().toUpperCase(Locale.ROOT);
        String term = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        int safePage = Math.max(1, page);
        int safePageSize = Math.min(50, Math.max(1, pageSize));

        List<ContentDescriptor> matches = new ArrayList<>();
        long total = 0;
        int candidateLimit = (int) Math.min(Integer.MAX_VALUE, (long) safePage * safePageSize);
        ContentCatalogFilter filter = new ContentCatalogFilter(status, level, term);
        for (String candidate : CONTENT_TYPES) {
            if (type != null && !type.equals(candidate)) continue;
            ContentCatalogSlice slice = content.catalog(EnglishContentType.valueOf(candidate), filter, candidateLimit);
            total += slice.total();
            matches.addAll(slice.items());
        }
        matches.sort(Comparator.comparingInt((ContentDescriptor descriptor) -> statusOrder(descriptor.publishStatus()))
                .thenComparing(descriptor -> descriptor.type().name())
                .thenComparingInt(ContentDescriptor::sortOrder)
                .thenComparingLong(ContentDescriptor::id));

        Set<Key> selected = repository.members(bundleId).stream()
                .map(member -> new Key(member.type(), member.contentId()))
                .collect(Collectors.toSet());
        long offset = ((long) safePage - 1) * safePageSize;
        List<BundleCatalogItemView> items = offset >= matches.size() ? List.of()
                : matches.subList((int) offset, (int) Math.min(matches.size(), offset + safePageSize)).stream()
                .map(descriptor -> new BundleCatalogItemView(
                        descriptor.type().name(), descriptor.id(), descriptor.title(), descriptor.slug(),
                        descriptor.summary(), descriptor.cefrLevel(), descriptor.coverUrl(),
                        descriptor.publishStatus(), descriptor.sortOrder(),
                        selected.contains(new Key(descriptor.type().name(), descriptor.id()))))
                .toList();
        int totalPages = total == 0 ? 0 : (int) ((total + safePageSize - 1) / safePageSize);
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
        LearningBundleItemRepository.Metadata metadata = repository.metadata(bundleId);

        Set<String> issues = new LinkedHashSet<>();
        if (metadata.summary() == null || metadata.summary().isBlank()) issues.add("SUMMARY_REQUIRED");
        if (metadata.primaryCefr() == null) issues.add("CEFR_REQUIRED");
        if (counts.getOrDefault("READING", 0) < 1) issues.add("READING_REQUIRED");
        if (counts.getOrDefault("LISTENING", 0) < 1) issues.add("LISTENING_REQUIRED");
        if (counts.getOrDefault("WRITING", 0) < 1) issues.add("WRITING_REQUIRED");
        if (published != items.size()) issues.add("UNPUBLISHED_ITEMS_PRESENT");
        return new BundleReadinessView(items.size(), published, modules, counts,
                issues.isEmpty(), List.copyOf(issues));
    }

    public void assertPublishable(Long bundleId) {
        if (!readiness(bundleId).ready()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_BUNDLE_NOT_READY",
                    "Bundle is not ready", "Complete the bundle publishing checklist before publishing.");
        }
    }

    /** Keeps already-published two-module bundles readable while enforcing current content validity. */
    public boolean publiclyAccessible(Long bundleId) {
        Set<String> legacyModuleIssues = Set.of("READING_REQUIRED", "LISTENING_REQUIRED", "WRITING_REQUIRED");
        return readiness(bundleId).issues().stream().allMatch(legacyModuleIssues::contains);
    }

    @Transactional
    public BundleItemView add(Long bundleId, String rawType, Long contentId) {
        requireMutableBundle(bundleId);
        String type = type(rawType);
        requireContent(type, contentId);
        int order = nextOrder(bundleId);
        try {
            repository.add(bundleId, type, contentId, order);
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
        if (repository.remove(bundleId, type, contentId) == 0) notFound();
        normalize(bundleId);
    }

    @Transactional
    public void move(Long bundleId, String rawType, Long contentId, int target) {
        requireMutableBundle(bundleId);
        String type = type(rawType);
        List<Key> keys = repository.members(bundleId).stream()
                .map(member -> new Key(member.type(), member.contentId()))
                .collect(Collectors.toCollection(ArrayList::new));
        Key selected = new Key(type, contentId);
        if (!keys.remove(selected)) notFound();
        keys.add(Math.min(Math.max(target, 0), keys.size()), selected);
        updateOrder(bundleId, keys);
    }

    public List<BundleItemView> publicList(String slug) {
        Long id;
        try {
            id = repository.publishedIdBySlug(slug);
        } catch (Exception ex) {
            throw unavailable();
        }
        if (!publiclyAccessible(id)) throw unavailable();
        return list(id, true);
    }

    private void normalize(Long bundleId) {
        updateOrder(bundleId, repository.members(bundleId).stream()
                .map(member -> new Key(member.type(), member.contentId())).toList());
    }

    private void updateOrder(Long bundleId, List<Key> keys) {
        for (int index = 0; index < keys.size(); index++) {
            Key key = keys.get(index);
            repository.updateOrder(bundleId, key.type(), key.id(), (index + 1) * 10);
        }
    }

    private void requireBundle(Long id, boolean published) {
        if (!repository.exists(id, published)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_BUNDLE_NOT_FOUND",
                    "Bundle not found", "The learning bundle does not exist.");
        }
    }

    private void requireMutableBundle(Long id) {
        requireBundle(id, false);
        if ("PUBLISHED".equals(repository.publishStatus(id))) {
            throw new ApiException(HttpStatus.CONFLICT, "ENGLISH_BUNDLE_PUBLISHED_LOCKED",
                    "Published bundle is locked", "Withdraw the bundle before changing its learning path.");
        }
    }

    private void requireContent(String type, Long id) {
        try {
            content.require(EnglishContentType.valueOf(type), id);
        } catch (ApiException ex) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_BUNDLE_ITEM_INVALID",
                    "Invalid bundle item", "The selected content does not exist.");
        }
    }

    private int nextOrder(Long bundleId) {
        return repository.members(bundleId).stream().mapToInt(LearningBundleItemRepository.Member::sortOrder)
                .max().orElse(0) + 10;
    }

    private int statusOrder(String status) {
        return switch (status) {
            case "PUBLISHED" -> 1;
            case "DRAFT" -> 2;
            case "WITHDRAWN" -> 3;
            default -> 0;
        };
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

    private BundleItemView toItem(ContentDescriptor descriptor, int sortOrder) {
        return new BundleItemView(descriptor.type().name(), descriptor.id(), descriptor.title(), descriptor.slug(),
                descriptor.summary(), descriptor.cefrLevel(), descriptor.coverUrl(),
                descriptor.publishStatus(), sortOrder);
    }

    private void notFound() {
        throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_BUNDLE_ITEM_NOT_FOUND",
                "Bundle item not found", "The selected bundle item does not exist.");
    }

    private ApiException unavailable() {
        return new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_CONTENT_NOT_PUBLISHED",
                "Bundle unavailable", "The learning bundle is not ready for public access.");
    }

    private record Key(String type, long id) { }
}
