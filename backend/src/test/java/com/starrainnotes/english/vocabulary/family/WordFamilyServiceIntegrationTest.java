package com.starrainnotes.english.vocabulary.family;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import com.starrainnotes.english.vocabulary.family.dto.WordFamilyMemberRequest;
import com.starrainnotes.english.vocabulary.family.dto.WordFamilyRequest;
import com.starrainnotes.english.vocabulary.family.service.WordFamilyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WordFamilyServiceIntegrationTest extends AbstractAuthIntegrationTest {
    private static final String SLUG = "word-family-refactor-fixture";

    @Autowired WordFamilyService service;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM vocabulary_word_family WHERE slug=?", SLUG);
    }

    @Test
    void persistsFamilyMembersAndTheirOrderThroughRepository() {
        cleanup();
        var family = service.create(new WordFamilyRequest("refactor family", SLUG, "fixture", List.of()));
        var first = service.addMember(family.id(),
                new WordFamilyMemberRequest("first", null, null, "one", null, null, null, null));
        var second = service.addMember(family.id(),
                new WordFamilyMemberRequest("second", null, null, "two", null, null, null, null));

        assertThat(service.publicGet(SLUG).members()).extracting("spelling")
                .containsExactly("first", "second");
        service.move(family.id(), second.id(), 0);
        assertThat(service.get(family.id()).members()).extracting("spelling")
                .containsExactly("second", "first");
        service.updateMember(family.id(), first.id(),
                new WordFamilyMemberRequest("updated", null, null, "one", null, null, null, null));
        assertThat(service.list("refactor family")).singleElement()
                .satisfies(view -> assertThat(view.members()).extracting("spelling")
                        .containsExactly("second", "updated"));
    }
}
