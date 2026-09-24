package com.starrainnotes.english.vocabulary.family.infrastructure;

import com.starrainnotes.english.vocabulary.family.dto.WordFamilyMemberView;
import com.starrainnotes.english.vocabulary.family.dto.WordFamilyView;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Repository
public class WordFamilyRepository {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;

    public WordFamilyRepository(JdbcTemplate jdbc, SiteSettingsTimezone timezone) {
        this.jdbc = jdbc;
        this.timezone = timezone;
    }

    public List<Long> ids(String query) {
        List<Object> args = new ArrayList<>();
        String where = "";
        if (query != null && !query.isBlank()) {
            where = " WHERE head_word LIKE ? OR slug LIKE ?";
            args.add("%" + query.trim() + "%");
            args.add("%" + query.trim() + "%");
        }
        return jdbc.queryForList("SELECT id FROM vocabulary_word_family" + where + " ORDER BY head_word,id",
                Long.class, args.toArray());
    }

    public WordFamilyView get(Long id) {
        try {
            return jdbc.queryForObject("SELECT * FROM vocabulary_word_family WHERE id=?",
                    (rs, row) -> new WordFamilyView(id, rs.getString("head_word"),
                            rs.getString("slug"), rs.getString("description"),
                            links(id), members(id),
                            timezone.atSite(rs.getTimestamp("updated_at").toLocalDateTime()).format(ISO)), id);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public Long idBySlug(String slug) {
        try {
            return jdbc.queryForObject("SELECT id FROM vocabulary_word_family WHERE slug=?", Long.class, slug);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public boolean slugExists(String slug) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM vocabulary_word_family WHERE slug=?",
                Integer.class, slug);
        return count != null && count > 0;
    }

    public long insert(String headWord, String slug, String description) {
        jdbc.update("INSERT INTO vocabulary_word_family(head_word,slug,description) VALUES (?,?,?)",
                headWord, slug, description);
        return jdbc.queryForObject("SELECT id FROM vocabulary_word_family WHERE slug=?", Long.class, slug);
    }

    public void update(Long id, String headWord, String slug, String description) {
        jdbc.update("UPDATE vocabulary_word_family SET head_word=?,slug=?,description=? WHERE id=?",
                headWord, slug, description, id);
    }

    public void delete(Long id) {
        jdbc.update("DELETE FROM vocabulary_word_family WHERE id=?", id);
    }

    public boolean wordExists(Long id) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM vocabulary_word WHERE id=?", Integer.class, id);
        return count != null && count > 0;
    }

    public void replaceLinks(Long familyId, List<Long> wordIds) {
        jdbc.update("DELETE FROM vocabulary_word_family_link WHERE family_id=?", familyId);
        for (Long wordId : wordIds) {
            jdbc.update("INSERT INTO vocabulary_word_family_link(family_id,word_id) VALUES (?,?)",
                    familyId, wordId);
        }
    }

    public boolean cefrExists(String cefr) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM english_cefr_standard WHERE level=?",
                Integer.class, cefr);
        return count != null && count > 0;
    }

    public int nextMemberOrder(Long familyId) {
        Integer max = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort_order),0) FROM vocabulary_family_member WHERE family_id=?",
                Integer.class, familyId);
        return (max == null ? 0 : max) + 10;
    }

    public Long insertMember(Long familyId, String spelling, String partOfSpeech, String phonetic,
                             String translation, String cefr, String example, String exampleTranslation,
                             int order) {
        jdbc.update("""
                INSERT INTO vocabulary_family_member(family_id,spelling,part_of_speech,phonetic_us,
                translation,cefr_level,example_sentence,example_translation,sort_order)
                VALUES (?,?,?,?,?,?,?,?,?)
                """, familyId, spelling, partOfSpeech, phonetic, translation, cefr,
                example, exampleTranslation, order);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public void updateMember(Long id, String spelling, String partOfSpeech, String phonetic,
                             String translation, String cefr, String example, String exampleTranslation,
                             Integer order) {
        jdbc.update("""
                UPDATE vocabulary_family_member
                SET spelling=?,part_of_speech=?,phonetic_us=?,translation=?,cefr_level=?,
                    example_sentence=?,example_translation=?,sort_order=COALESCE(?,sort_order)
                WHERE id=?
                """, spelling, partOfSpeech, phonetic, translation, cefr,
                example, exampleTranslation, order, id);
    }

    public void deleteMember(Long id) {
        jdbc.update("DELETE FROM vocabulary_family_member WHERE id=?", id);
    }

    public boolean ownsMember(Long familyId, Long memberId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM vocabulary_family_member WHERE family_id=? AND id=?",
                Integer.class, familyId, memberId);
        return count != null && count > 0;
    }

    public List<Long> memberIds(Long familyId) {
        return jdbc.queryForList(
                "SELECT id FROM vocabulary_family_member WHERE family_id=? ORDER BY sort_order,id",
                Long.class, familyId);
    }

    public void setMemberOrder(Long memberId, int order) {
        jdbc.update("UPDATE vocabulary_family_member SET sort_order=? WHERE id=?", order, memberId);
    }

    public WordFamilyMemberView member(Long id) {
        try {
            return jdbc.queryForObject("SELECT * FROM vocabulary_family_member WHERE id=?",
                    (rs, row) -> mapMember(rs), id);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private List<Long> links(Long familyId) {
        return jdbc.queryForList(
                "SELECT word_id FROM vocabulary_word_family_link WHERE family_id=? ORDER BY word_id",
                Long.class, familyId);
    }

    private List<WordFamilyMemberView> members(Long familyId) {
        return jdbc.query("SELECT * FROM vocabulary_family_member WHERE family_id=? ORDER BY sort_order,id",
                (rs, row) -> mapMember(rs), familyId);
    }

    private WordFamilyMemberView mapMember(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new WordFamilyMemberView(rs.getLong("id"), rs.getString("spelling"),
                rs.getString("part_of_speech"), rs.getString("phonetic_us"),
                rs.getString("translation"), rs.getString("cefr_level"),
                rs.getString("example_sentence"), rs.getString("example_translation"),
                rs.getInt("sort_order"));
    }
}
