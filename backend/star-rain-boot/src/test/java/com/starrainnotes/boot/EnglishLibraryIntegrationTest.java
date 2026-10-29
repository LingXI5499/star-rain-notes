package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.library.dto.LibraryDto.Classification;
import com.starrainnotes.english.library.mapper.LibraryMapper;
import com.starrainnotes.english.library.service.EnglishLibraryService;
import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import com.starrainnotes.english.taxonomy.mapper.TaxonomyMapper;
import com.starrainnotes.english.taxonomy.service.impl.TaxonomyServiceImpl;
import com.starrainnotes.english.vocabulary.service.VocabularyService;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;

/** Real MySQL contracts; every fixture is rolled back in the isolated test schema. */
class EnglishLibraryIntegrationTest extends MapperXmlIntegrationSupport {
    @Test
    void confirmedMappingsDriveCountsAndPreserveLearningIdentity() throws Exception {
        try (SqlSession session = openSession()) {
            var taxonomy = new TaxonomyServiceImpl(session.getMapper(TaxonomyMapper.class));
            Node root = taxonomy.create(term("T98", null));
            Node topic = taxonomy.create(term("T98-01", root.getId()));
            LibraryMapper mapper = session.getMapper(LibraryMapper.class);
            var service = new EnglishLibraryService(mapper, taxonomy, mock(VocabularyService.class), new ObjectMapper());
            try (Statement sql = session.getConnection().createStatement()) {
                sql.executeUpdate("INSERT INTO sr_english_vocabulary_theme(layer,layer_order,name,sort_order) VALUES('Probe',1,'Library probe',1)", Statement.RETURN_GENERATED_KEYS);
                long theme; try (var keys = sql.getGeneratedKeys()) { keys.next(); theme = keys.getLong(1); }
                sql.executeUpdate("INSERT INTO sr_english_vocabulary_word(theme_id,word,part_of_speech,translation,examples,sort_order) VALUES("+theme+",'library-probe-word','n.','测试词义','[]',1)", Statement.RETURN_GENERATED_KEYS);
                long id; try (var keys = sql.getGeneratedKeys()) { keys.next(); id = keys.getLong(1); }
                sql.executeUpdate("INSERT INTO sr_english_vocabulary_mapping(word_id,entry_id,legacy_index,migration_status) VALUES("+id+",'probe-"+UUID.randomUUID()+"',0,'needs-review')");
                assertEquals(1, service.words("library-probe-word", null, null, "needs-review", 1, 24).getTotal());
                assertEquals(0, service.words("library-probe-word", "T98", null, "confirmed", 1, 24).getTotal());
                Classification value = new Classification(); value.setMigrationStatus("confirmed"); value.setMigrationReason("Reviewed meaning"); value.setTopicCodes(List.of("T98-01")); value.setRowVersion(0L);
                service.classify(id, value);
                var result = service.words("library-probe-word", "t98", "t98-01", "confirmed", 1, 24);
                assertEquals(1, result.getTotal()); assertEquals(id, result.getItems().getFirst().getId());
                assertEquals(List.of("T98-01"), result.getItems().getFirst().getTopicCodes());
                assertEquals(1L, service.counts("vocabulary").get("T98"));
                assertThrows(ApiException.class, () -> service.classify(id, value));
                assertThrows(ApiException.class, () -> service.words("", "T98", "T01-01", "confirmed", 1, 24));
                assertThrows(ApiException.class, () -> service.words("", null, null, "unknown", 1, 24));
                assertThrows(ApiException.class, () -> service.words("", null, null, "", 0, 24));
                assertEquals(0, service.words("%", "T98", null, "confirmed", 1, 24).getTotal());
                value.setRowVersion(1L); value.setMigrationStatus("needs-review"); value.setTopicCodes(List.of()); service.classify(id, value);
                assertEquals(0L, service.counts("vocabulary").get("T98"));
                assertEquals(1, service.words("library-probe-word", null, null, "needs-review", 1, 24).getTotal());
            } finally { session.rollback(true); }
        }
    }

    @Test
    void journalFacetsExcludePrivateDeletedAndIneligibleWriting() throws Exception {
        try (SqlSession session = openSession()) {
            var mapper = session.getMapper(LibraryMapper.class);
            String marker = "essay-probe-" + UUID.randomUUID();
            try (var sql = session.getConnection().prepareStatement("INSERT INTO sr_english_writing_article(owner_account_id,public_eligible,slug,title,summary,body_markdown,state,visibility,keywords_json,published_at,deleted_at) VALUES(1,?,?,?,?,?,'COMPLETED',?,?,?,?)")) {
                for (int i = 0; i < 4; i++) {
                    sql.setBoolean(1, i != 2); sql.setString(2, marker + i); sql.setString(3, marker); sql.setString(4, "Summary"); sql.setString(5, "A real body.");
                    sql.setString(6, i == 1 ? "PRIVATE" : "PUBLIC"); sql.setString(7, "[\"" + marker + "\"]"); sql.setString(8, "2026-10-20 10:00:00");
                    if (i == 3) { sql.setString(9, "2026-10-21 10:00:00"); } else { sql.setNull(9, java.sql.Types.TIMESTAMP); }
                    sql.executeUpdate();
                }
                assertEquals(1, mapper.countEssays(marker, marker, "2026-10", ""));
                assertEquals(0, mapper.countEssays(marker, marker, "2026-09", ""));
                assertEquals(1, mapper.essays(marker, marker, "2026-10", "", 0, 10).size());
                assertTrue(mapper.essayTags().stream().anyMatch(row -> marker.equals(row.get("name")) && ((Number) row.get("total")).longValue() == 1));
                var service = new EnglishLibraryService(mapper, mock(TaxonomyServiceImpl.class), mock(VocabularyService.class), new ObjectMapper());
                assertEquals(List.of(marker), service.essays(marker, marker, "2026-10", "", 1, 10).getItems().getFirst().getKeywords());
                assertThrows(ApiException.class, () -> service.essays("", "", "2026-13", "", 1, 10));
            } finally { session.rollback(true); }
        }
    }

    private static Node term(String code, Long parent) {
        Node node = new Node(); node.setDimension("VOCABULARY"); node.setLibraryModule("vocabulary"); node.setCode(code);
        node.setParentId(parent); node.setName(code); node.setNameEn(code); node.setSlug("library-probe-"+UUID.randomUUID()); node.setSortOrder(1);
        return node;
    }
}
