package com.starrainnotes.english.learning;

import com.starrainnotes.auth.AbstractAuthIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EnglishLearningIntegrationTest extends AbstractAuthIntegrationTest {
    private static final String KEY="5bf41b93-274d-4a9f-a7d9-75c8ace8a216";
    @Autowired JdbcTemplate jdbc;
    private Long promptId;

    @BeforeEach void seed(){
        jdbc.update("""
          INSERT INTO english_writing_prompt(title,slug,summary,background_markdown,requirements_markdown,
          cefr_level,word_min,word_max,estimated_minutes,publish_status,sort_order,published_at)
          VALUES ('Learning prompt','learning-prompt','summary','background','requirements',
          'B1',80,120,20,'PUBLISHED',10,UTC_TIMESTAMP(6))
          """);
        promptId=jdbc.queryForObject("SELECT id FROM english_writing_prompt WHERE slug='learning-prompt'",Long.class);
    }
    @AfterEach void cleanup(){
        jdbc.update("DELETE FROM english_writing_submission");
        jdbc.update("DELETE FROM english_learning_attempt");
        jdbc.update("DELETE FROM english_learning_record");
        jdbc.update("DELETE FROM english_learner_profile");
        jdbc.update("DELETE FROM english_learning_bundle WHERE slug LIKE 'learning-test-%'");
        jdbc.update("DELETE FROM english_listening_item WHERE slug LIKE 'learning-test-%'");
        jdbc.update("DELETE FROM english_reading_article WHERE slug LIKE 'learning-test-%'");
        jdbc.update("DELETE FROM english_writing_prompt WHERE slug='learning-prompt'");
    }

    @Test void savesProgressAndBuildsSummary() throws Exception {
        mockMvc.perform(withCsrf(put("/api/v1/public/english/learning/records/writing/"+promptId)
                        .header("X-Learner-Key",KEY).contentType("application/json")
                        .content("{\"status\":\"COMPLETED\",\"score\":88,\"timeSpentSeconds\":300,\"weakPoints\":[\"cohesion\"],\"mastery\":0.82}"),fetchCsrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptCount").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.nextReviewAt").isNotEmpty());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM english_learning_attempt",Integer.class)).isEqualTo(1);
        mockMvc.perform(get("/api/v1/public/english/learning/summary").header("X-Learner-Key",KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(1))
                .andExpect(jsonPath("$.completedByType.WRITING").value(1));
        jdbc.update("UPDATE english_learning_record SET next_review_at=UTC_TIMESTAMP(6)-INTERVAL 1 DAY");
        mockMvc.perform(get("/api/v1/public/english/learning/insights").header("X-Learner-Key",KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTimeSeconds").value(300))
                .andExpect(jsonPath("$.totalAttempts").value(1))
                .andExpect(jsonPath("$.activeDays14").value(1))
                .andExpect(jsonPath("$.modules[?(@.contentType=='WRITING')].completed").value(1))
                .andExpect(jsonPath("$.recommendations[0].reason").value("到期复习"))
                .andExpect(jsonPath("$.recommendations[0].route").value("/english/writing/practice/learning-prompt"));
    }

    @Test void writingDraftIsUpsertedAndSubmitted() throws Exception {
        mockMvc.perform(withCsrf(put("/api/v1/public/english/learning/writing-submissions/"+promptId)
                        .header("X-Learner-Key",KEY).contentType("application/json")
                        .content("{\"bodyText\":\"This is my first draft.\",\"status\":\"DRAFT\"}"),fetchCsrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wordCount").value(5))
                .andExpect(jsonPath("$.status").value("DRAFT"));
        mockMvc.perform(withCsrf(put("/api/v1/public/english/learning/writing-submissions/"+promptId)
                        .header("X-Learner-Key",KEY).contentType("application/json")
                        .content("{\"bodyText\":\"This is my final response.\",\"status\":\"SUBMITTED\",\"selfScore\":80}"),fetchCsrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.submittedAt").isNotEmpty());
    }

    @Test void returnsMultipleLearningRecordsInOneRequest() throws Exception {
        saveRecord("WRITING",promptId,"IN_PROGRESS");
        mockMvc.perform(get("/api/v1/public/english/learning/records/batch")
                        .header("X-Learner-Key",KEY)
                        .param("ref","WRITING:"+promptId,"READING:999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['WRITING:"+promptId+"'].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test void recommendsBundleNextStepAndExplicitReadingListeningPair() throws Exception {
        Long reading=seedReading("learning-test-reading");
        Long listening=seedListening("learning-test-listening");
        jdbc.update("INSERT INTO english_reading_listening_pair(reading_article_id,listening_item_id,relation_type,sort_order) VALUES (?,?,'SAME_TOPIC',10)",reading,listening);
        jdbc.update("INSERT INTO english_learning_bundle(title,slug,summary,primary_cefr,publish_status,sort_order,published_at) VALUES ('Test path','learning-test-bundle','A complete path','B1','PUBLISHED',10,UTC_TIMESTAMP(6))");
        Long bundle=jdbc.queryForObject("SELECT id FROM english_learning_bundle WHERE slug='learning-test-bundle'",Long.class);
        jdbc.update("INSERT INTO english_learning_bundle_reading_item(bundle_id,article_id,sort_order) VALUES (?,?,10)",bundle,reading);
        jdbc.update("INSERT INTO english_learning_bundle_writing_item(bundle_id,prompt_id,sort_order) VALUES (?,?,20)",bundle,promptId);
        saveRecord("READING",reading,"COMPLETED");

        mockMvc.perform(get("/api/v1/public/english/learning/insights").header("X-Learner-Key",KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[?(@.recommendationType=='BUNDLE_NEXT' && @.contentType=='WRITING')].sourceTitle").value("Test path"))
                .andExpect(jsonPath("$.recommendations[?(@.recommendationType=='PAIRED' && @.contentType=='LISTENING')].sourceTitle").value("Learning reading"));
    }

    @Test void recommendsUnseenContentWithSharedSemanticTags() throws Exception {
        Long reading=seedReading("learning-test-tag-source");
        Long term=jdbc.queryForObject("SELECT id FROM english_taxonomy_term WHERE dimension='TOPIC' ORDER BY id LIMIT 1",Long.class);
        jdbc.update("INSERT INTO english_reading_article_tag(article_id,term_id,tag_role) VALUES (?,?,'PRIMARY')",reading,term);
        jdbc.update("INSERT INTO english_writing_prompt_tag(prompt_id,term_id) VALUES (?,?)",promptId,term);
        saveRecord("READING",reading,"COMPLETED");

        mockMvc.perform(get("/api/v1/public/english/learning/insights").header("X-Learner-Key",KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[?(@.recommendationType=='TAG_MATCH' && @.contentType=='WRITING')].title").value("Learning prompt"));
    }

    @Test void rejectsMissingLearnerKeyAndUnpublishedContent() throws Exception {
        mockMvc.perform(get("/api/v1/public/english/learning/summary"))
                .andExpect(status().isBadRequest());
        jdbc.update("UPDATE english_writing_prompt SET publish_status='WITHDRAWN' WHERE id=?",promptId);
        mockMvc.perform(withCsrf(put("/api/v1/public/english/learning/records/writing/"+promptId)
                        .header("X-Learner-Key",KEY).contentType("application/json")
                        .content("{\"status\":\"IN_PROGRESS\"}"),fetchCsrfToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENGLISH_CONTENT_NOT_PUBLISHED"));
    }

    private void saveRecord(String type,Long id,String recordStatus) throws Exception {
        mockMvc.perform(withCsrf(put("/api/v1/public/english/learning/records/"+type+"/"+id)
                        .header("X-Learner-Key",KEY).contentType("application/json")
                        .content("{\"status\":\""+recordStatus+"\",\"timeSpentSeconds\":60,\"mastery\":0.7}"),fetchCsrfToken()))
                .andExpect(status().isOk());
    }

    private Long seedReading(String slug){
        jdbc.update("""
          INSERT INTO english_reading_article(title,slug,summary,body_markdown,reading_level,cefr_level,
          publish_status,sort_order,published_at) VALUES ('Learning reading',?,'summary','body',1,'B1','PUBLISHED',10,UTC_TIMESTAMP(6))
          """,slug);
        return jdbc.queryForObject("SELECT id FROM english_reading_article WHERE slug=?",Long.class,slug);
    }

    private Long seedListening(String slug){
        jdbc.update("""
          INSERT INTO english_listening_item(title,slug,summary,transcript_markdown,cefr_level,listening_level,
          duration_seconds,publish_status,sort_order,published_at) VALUES ('Learning listening',?,'summary','body','B1',1,60,'PUBLISHED',10,UTC_TIMESTAMP(6))
          """,slug);
        return jdbc.queryForObject("SELECT id FROM english_listening_item WHERE slug=?",Long.class,slug);
    }
}
