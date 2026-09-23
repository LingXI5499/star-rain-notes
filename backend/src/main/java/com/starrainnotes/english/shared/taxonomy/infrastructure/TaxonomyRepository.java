package com.starrainnotes.english.shared.taxonomy.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.english.shared.taxonomy.entity.EnglishTaxonomyTerm;
import com.starrainnotes.english.shared.taxonomy.mapper.EnglishTaxonomyTermMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Persistence and reference checks for the shared taxonomy. */
@Repository
public class TaxonomyRepository {
    private static final List<String> CONTENT_REFERENCE_TABLES = List.of(
            "english_reading_article_tag", "english_listening_item_tag",
            "english_writing_resource_tag", "english_writing_prompt_tag");

    private final EnglishTaxonomyTermMapper mapper;
    private final JdbcTemplate jdbc;

    public TaxonomyRepository(EnglishTaxonomyTermMapper mapper, JdbcTemplate jdbc) {
        this.mapper = mapper;
        this.jdbc = jdbc;
    }

    public List<EnglishTaxonomyTerm> all() {
        return mapper.selectList(new LambdaQueryWrapper<EnglishTaxonomyTerm>()
                .orderByAsc(EnglishTaxonomyTerm::getDimension)
                .orderByAsc(EnglishTaxonomyTerm::getSortOrder)
                .orderByAsc(EnglishTaxonomyTerm::getId));
    }

    public EnglishTaxonomyTerm find(Long id) {
        return mapper.selectById(id);
    }

    public EnglishTaxonomyTerm findBySlug(String slug) {
        return mapper.selectOne(new LambdaQueryWrapper<EnglishTaxonomyTerm>()
                .eq(EnglishTaxonomyTerm::getSlug, slug).last("LIMIT 1"));
    }

    public void insert(EnglishTaxonomyTerm term) {
        mapper.insert(term);
    }

    public void update(EnglishTaxonomyTerm term) {
        mapper.updateById(term);
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }

    public long countChildren(Long parentId) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<EnglishTaxonomyTerm>()
                .eq(EnglishTaxonomyTerm::getParentId, parentId));
        return count == null ? 0 : count;
    }

    public int nextOrder(String dimension, Long parentId) {
        LambdaQueryWrapper<EnglishTaxonomyTerm> wrapper = siblingsQuery(dimension, parentId);
        return mapper.selectList(wrapper).stream()
                .map(EnglishTaxonomyTerm::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo).orElse(0) + 10;
    }

    public List<Long> siblingIds(String dimension, Long parentId) {
        LambdaQueryWrapper<EnglishTaxonomyTerm> wrapper = siblingsQuery(dimension, parentId);
        wrapper.orderByAsc(EnglishTaxonomyTerm::getSortOrder).orderByAsc(EnglishTaxonomyTerm::getId);
        return new ArrayList<>(mapper.selectList(wrapper).stream().map(EnglishTaxonomyTerm::getId).toList());
    }

    public void normalizeOrder(List<Long> ids) {
        if (ids.isEmpty()) return;
        jdbc.update("UPDATE english_taxonomy_term SET sort_order=100000 WHERE id IN ("
                + String.join(",", ids.stream().map(String::valueOf).toList()) + ")");
        for (int index = 0; index < ids.size(); index++) {
            jdbc.update("UPDATE english_taxonomy_term SET sort_order=? WHERE id=?",
                    (index + 1) * 10, ids.get(index));
        }
    }

    public boolean slugExists(String slug, Long excludedId) {
        LambdaQueryWrapper<EnglishTaxonomyTerm> wrapper = new LambdaQueryWrapper<EnglishTaxonomyTerm>()
                .eq(EnglishTaxonomyTerm::getSlug, slug);
        if (excludedId != null) wrapper.ne(EnglishTaxonomyTerm::getId, excludedId);
        Long count = mapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    public boolean referencedByContent(Long termId) {
        return usageCount(termId) > 0;
    }

    public long usageCount(Long termId) {
        long total = 0;
        for (String table : CONTENT_REFERENCE_TABLES) {
            if (!tableExists(table)) continue;
            try {
                Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE term_id=?",
                        Long.class, termId);
                if (count != null) total += count;
            } catch (DataAccessException ignored) {
                // Earlier schema revisions may lack this mapping column.
            }
        }
        return total;
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

    private LambdaQueryWrapper<EnglishTaxonomyTerm> siblingsQuery(String dimension, Long parentId) {
        LambdaQueryWrapper<EnglishTaxonomyTerm> wrapper = new LambdaQueryWrapper<EnglishTaxonomyTerm>()
                .eq(EnglishTaxonomyTerm::getDimension, dimension);
        if (parentId == null) wrapper.isNull(EnglishTaxonomyTerm::getParentId);
        else wrapper.eq(EnglishTaxonomyTerm::getParentId, parentId);
        return wrapper;
    }
}
