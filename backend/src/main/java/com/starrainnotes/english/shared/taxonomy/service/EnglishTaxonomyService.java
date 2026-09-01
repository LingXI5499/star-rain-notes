package com.starrainnotes.english.shared.taxonomy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.english.shared.taxonomy.dto.TaxonomyMoveRequest;
import com.starrainnotes.english.shared.taxonomy.dto.TaxonomyRequest;
import com.starrainnotes.english.shared.taxonomy.dto.TaxonomyTermView;
import com.starrainnotes.english.shared.taxonomy.entity.EnglishTaxonomyTerm;
import com.starrainnotes.english.shared.taxonomy.mapper.EnglishTaxonomyTermMapper;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Unified semantic taxonomy management (方案 §5.1, §10.2, §11).
 *
 * <p>Invariants enforced here (never delegated to the DB): terms are at most
 * two levels, a child shares its parent's dimension, no cycles, slug globally
 * unique, sibling ordering normalised to 10/20/30 via a two-phase temporary
 * value. Deleting an in-use term (it has children, or a module mapping table
 * references it) returns 409 {@code ENGLISH_TAXONOMY_IN_USE}.</p>
 */
@Service
public class EnglishTaxonomyService {

    public static final Set<String> DIMENSIONS =
            Set.of("TOPIC", "SCENE", "FUNCTION", "ABILITY", "GENRE", "FORMAT");

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String IN_USE = "ENGLISH_TAXONOMY_IN_USE";

    private final EnglishTaxonomyTermMapper mapper;
    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;

    public EnglishTaxonomyService(EnglishTaxonomyTermMapper mapper,
                                  JdbcTemplate jdbc,
                                  SiteSettingsTimezone timezone) {
        this.mapper = mapper;
        this.jdbc = jdbc;
        this.timezone = timezone;
    }

    public List<TaxonomyTermView> tree() {
        List<EnglishTaxonomyTerm> all = mapper.selectList(new LambdaQueryWrapper<EnglishTaxonomyTerm>()
                .orderByAsc(EnglishTaxonomyTerm::getDimension)
                .orderByAsc(EnglishTaxonomyTerm::getSortOrder)
                .orderByAsc(EnglishTaxonomyTerm::getId));
        Map<Long, TaxonomyTermView> views = new LinkedHashMap<>();
        for (EnglishTaxonomyTerm term : all) {
            views.put(term.getId(), toView(term, List.of()));
        }
        for (EnglishTaxonomyTerm term : all) {
            if (term.getParentId() == null) continue;
            TaxonomyTermView parent = views.get(term.getParentId());
            if (parent == null) continue;
            List<TaxonomyTermView> children = new ArrayList<>(parent.children());
            children.add(views.get(term.getId()));
            views.put(term.getParentId(), new TaxonomyTermView(parent.id(), parent.parentId(),
                    parent.dimension(), parent.name(), parent.slug(), parent.description(),
                    parent.sortOrder(), parent.enabled(), parent.updatedAt(), children));
        }
        List<TaxonomyTermView> roots = new ArrayList<>();
        for (EnglishTaxonomyTerm term : all) {
            if (term.getParentId() == null) roots.add(views.get(term.getId()));
        }
        return roots;
    }

    public List<TaxonomyTermView> flat() {
        return mapper.selectList(new LambdaQueryWrapper<EnglishTaxonomyTerm>()
                .orderByAsc(EnglishTaxonomyTerm::getDimension)
                .orderByAsc(EnglishTaxonomyTerm::getSortOrder)
                .orderByAsc(EnglishTaxonomyTerm::getId)).stream()
                .map(term -> toView(term, null))
                .toList();
    }

    @Transactional
    public TaxonomyTermView create(TaxonomyRequest request) {
        String dimension = requireDimension(request.dimension());
        Long parentId = request.parentId();
        if (parentId != null) {
            EnglishTaxonomyTerm parent = require(parentId);
            requireSameDimension(parent, dimension);
            if (parent.getParentId() != null) {
                throw depthInvalid("A taxonomy term can be at most two levels deep.");
            }
        }
        String slug = NumericSlugGenerator.forCreate(request.slug(), candidate -> slugExists(candidate, null));
        assertSlugFree(slug, null);
        Integer order = request.sortOrder() == null ? nextOrder(dimension, parentId) : request.sortOrder();
        if (order < 1) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_DEPTH_INVALID",
                    "Invalid sort order", "Sort order must be a positive integer.");
        }
        EnglishTaxonomyTerm term = new EnglishTaxonomyTerm();
        term.setDimension(dimension);
        term.setParentId(parentId);
        term.setName(request.name().trim());
        term.setSlug(slug);
        term.setDescription(clean(request.description()));
        term.setSortOrder(order);
        term.setEnabled(request.enabled() == null || request.enabled());
        mapper.insert(term);
        return toView(require(term.getId()), null);
    }

    @Transactional
    public TaxonomyTermView update(Long id, TaxonomyRequest request) {
        EnglishTaxonomyTerm term = require(id);
        String dimension = requireDimension(request.dimension());
        boolean hasChildren = countChildren(id) > 0;

        if (hasChildren && !dimension.equals(term.getDimension())) {
            throw depthInvalid("A taxonomy term that has children cannot change dimension.");
        }
        if (hasChildren && request.parentId() != null) {
            throw depthInvalid("A taxonomy term that has children must stay a root.");
        }

        Long parentId = request.parentId();
        if (parentId != null && parentId.equals(id)) {
            throw depthInvalid("A taxonomy term cannot be its own parent.");
        }
        if (parentId != null) {
            EnglishTaxonomyTerm parent = require(parentId);
            requireSameDimension(parent, dimension);
            if (parent.getParentId() != null) {
                throw depthInvalid("A taxonomy term can be at most two levels deep.");
            }
        }
        String slug = NumericSlugGenerator.forUpdate(request.slug(), term.getSlug());
        assertSlugFree(slug, id);
        if (request.sortOrder() != null && request.sortOrder() < 1) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_DEPTH_INVALID",
                    "Invalid sort order", "Sort order must be a positive integer.");
        }

        term.setDimension(dimension);
        term.setParentId(parentId);
        term.setName(request.name().trim());
        term.setSlug(slug);
        term.setDescription(clean(request.description()));
        if (request.sortOrder() != null) {
            term.setSortOrder(request.sortOrder());
        }
        if (request.enabled() != null) {
            term.setEnabled(request.enabled());
        }
        mapper.updateById(term);
        return toView(require(id), null);
    }

    @Transactional
    public void move(Long id, TaxonomyMoveRequest request) {
        EnglishTaxonomyTerm term = require(id);
        List<Long> ids = siblingIds(term.getDimension(), term.getParentId());
        ids.remove(id);
        int index = Math.min(request.targetIndex() == null ? 0 : request.targetIndex(), ids.size());
        ids.add(index, id);
        normalizeOrder(ids);
    }

    @Transactional
    public void delete(Long id) {
        EnglishTaxonomyTerm term = require(id);
        if (countChildren(id) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, IN_USE,
                    "Taxonomy term in use",
                    "This term still has children. Move or delete them first.");
        }
        if (referencedByContent(id)) {
            throw new ApiException(HttpStatus.CONFLICT, IN_USE,
                    "Taxonomy term in use",
                    "This term is referenced by published content and cannot be deleted.");
        }
        mapper.deleteById(id);
    }

    // ---------------------------------------------------------------
    // lookups
    // ---------------------------------------------------------------

    public EnglishTaxonomyTerm require(Long id) {
        EnglishTaxonomyTerm term = mapper.selectById(id);
        if (term == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_TAXONOMY_NOT_FOUND",
                    "Taxonomy term not found", "The taxonomy term does not exist.");
        }
        return term;
    }

    public EnglishTaxonomyTerm requireBySlug(String slug) {
        EnglishTaxonomyTerm term = mapper.selectOne(new LambdaQueryWrapper<EnglishTaxonomyTerm>()
                .eq(EnglishTaxonomyTerm::getSlug, slug)
                .last("LIMIT 1"));
        if (term == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_TAXONOMY_NOT_FOUND",
                    "Taxonomy term not found", "The taxonomy term does not exist.");
        }
        return term;
    }

    /**
     * Whether a term is referenced by a module mapping table. Reading / listening
     * / writing mapping tables land in V10–V12; this scans whatever reference
     * tables currently exist so the "delete in use" contract goes live as soon
     * as a phase lands, without editing this file.
     */
    private boolean referencedByContent(Long termId) {
        for (String table : contentReferenceTables()) {
            if (!tableExists(table)) continue;
            try {
                Long count = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM " + table + " WHERE term_id=?", Long.class, termId);
                if (count != null && count > 0) return true;
            } catch (DataAccessException ignored) {
                // column may be absent in an early phase; skip this table
            }
        }
        return false;
    }

    private List<String> contentReferenceTables() {
        return List.of("english_reading_article_tag",
                "english_listening_item_tag",
                "english_writing_resource_tag",
                "english_writing_prompt_tag");
    }

    private boolean tableExists(String table) {
        try {
            Long count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?",
                    Long.class, table);
            return count != null && count > 0;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    private long countChildren(Long parentId) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<EnglishTaxonomyTerm>()
                .eq(EnglishTaxonomyTerm::getParentId, parentId));
        return count == null ? 0 : count;
    }

    private Integer nextOrder(String dimension, Long parentId) {
        LambdaQueryWrapper<EnglishTaxonomyTerm> wrapper = new LambdaQueryWrapper<EnglishTaxonomyTerm>()
                .eq(EnglishTaxonomyTerm::getDimension, dimension);
        if (parentId == null) wrapper.isNull(EnglishTaxonomyTerm::getParentId);
        else wrapper.eq(EnglishTaxonomyTerm::getParentId, parentId);
        return mapper.selectList(wrapper).stream()
                .map(EnglishTaxonomyTerm::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 10;
    }

    private List<Long> siblingIds(String dimension, Long parentId) {
        LambdaQueryWrapper<EnglishTaxonomyTerm> wrapper = new LambdaQueryWrapper<EnglishTaxonomyTerm>()
                .eq(EnglishTaxonomyTerm::getDimension, dimension);
        if (parentId == null) {
            wrapper.isNull(EnglishTaxonomyTerm::getParentId);
        } else {
            wrapper.eq(EnglishTaxonomyTerm::getParentId, parentId);
        }
        wrapper.orderByAsc(EnglishTaxonomyTerm::getSortOrder).orderByAsc(EnglishTaxonomyTerm::getId);
        return new ArrayList<>(mapper.selectList(wrapper).stream()
                .map(EnglishTaxonomyTerm::getId)
                .toList());
    }

    private void normalizeOrder(List<Long> ids) {
        if (ids.isEmpty()) return;
        jdbc.update("UPDATE english_taxonomy_term SET sort_order=100000 WHERE id IN ("
                + String.join(",", ids.stream().map(String::valueOf).toList()) + ")");
        for (int index = 0; index < ids.size(); index++) {
            jdbc.update("UPDATE english_taxonomy_term SET sort_order=? WHERE id=?",
                    (index + 1) * 10, ids.get(index));
        }
    }

    private void assertSlugFree(String slug, Long excludedId) {
        LambdaQueryWrapper<EnglishTaxonomyTerm> wrapper = new LambdaQueryWrapper<EnglishTaxonomyTerm>()
                .eq(EnglishTaxonomyTerm::getSlug, slug);
        if (excludedId != null) {
            wrapper.ne(EnglishTaxonomyTerm::getId, excludedId);
        }
        Long count = mapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "ENGLISH_CONTENT_SLUG_CONFLICT",
                    "Slug already in use", "Choose another stable slug.");
        }
    }

    private boolean slugExists(String slug, Long excludedId) {
        LambdaQueryWrapper<EnglishTaxonomyTerm> wrapper = new LambdaQueryWrapper<EnglishTaxonomyTerm>()
                .eq(EnglishTaxonomyTerm::getSlug, slug);
        if (excludedId != null) wrapper.ne(EnglishTaxonomyTerm::getId, excludedId);
        Long count = mapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private String requireDimension(String dimension) {
        if (dimension == null || !DIMENSIONS.contains(dimension.trim())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_DEPTH_INVALID",
                    "Invalid dimension",
                    "Dimension must be one of TOPIC/SCENE/FUNCTION/ABILITY/GENRE/FORMAT.");
        }
        return dimension.trim();
    }

    private void requireSameDimension(EnglishTaxonomyTerm parent, String dimension) {
        if (!parent.getDimension().equals(dimension)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_DEPTH_INVALID",
                    "Cross-dimension parent",
                    "A child term must share its parent's dimension.");
        }
    }

    private ApiException depthInvalid(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_DEPTH_INVALID",
                "Invalid taxonomy hierarchy", detail);
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private TaxonomyTermView toView(EnglishTaxonomyTerm term, List<TaxonomyTermView> children) {
        return new TaxonomyTermView(term.getId(), term.getParentId(), term.getDimension(), term.getName(),
                term.getSlug(), term.getDescription(), term.getSortOrder(), term.getEnabled(),
                format(term.getUpdatedAt()), children);
    }

    private String format(LocalDateTime timestamp) {
        if (timestamp == null) return null;
        return timezone.atSite(timestamp).format(ISO_OFFSET);
    }
}
