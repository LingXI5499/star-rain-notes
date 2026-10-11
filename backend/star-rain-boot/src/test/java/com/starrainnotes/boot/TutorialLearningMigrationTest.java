package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** Applies the original schema and new migrations against an isolated, disposable database. */
class TutorialLearningMigrationTest extends MapperXmlIntegrationSupport {
    @Test void migrationPreservesLegacyAnswersResultsAndCompletionFacts() throws Exception {
        try(SqlSession session=openSession()) {
            Connection connection=session.getConnection();String original=connection.getCatalog();
            String schema="sr_learning_migration_probe_"+Long.toUnsignedString(System.nanoTime());
            assertTrue(schema.matches("sr_learning_migration_probe_[0-9]+"));
            boolean created=false;
            try(Statement admin=connection.createStatement()) {
                try {
                    admin.execute("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4");created=true;
                    connection.setCatalog(schema);assertEquals(schema,connection.getCatalog());
                    // Connector/J statements capture the catalog at creation. Create probes after selecting the isolated schema.
                    try(Statement statement=connection.createStatement()) {
                    apply(connection,"V2_001__account.sql");apply(connection,"V2_009__tutorial_content.sql");apply(connection,"V2_010__tutorial_learning.sql");
                    statement.executeUpdate("INSERT INTO sr_tutorial_chapter(id,tutorial_id,group_id,slug,title,body_markdown,sort_order,status) VALUES(1,1,1,'probe','Probe','# Body',0,'ACTIVE')");
                    statement.executeUpdate("INSERT INTO sr_tutorial_knowledge_card(id,chapter_id,front_text,back_markdown,sort_order,status) VALUES(1,1,'Recall','Answer',0,'ENABLED')");
                    statement.executeUpdate("INSERT INTO sr_user_question_answer(account_id,question_id,answer_text) VALUES(42,1,'Original answer')");
                    statement.executeUpdate("INSERT INTO sr_learning_progress(account_id,tutorial_id,group_id,chapter_id,completed_at) VALUES(42,1,1,1,CURRENT_TIMESTAMP(3))");
                    statement.executeUpdate("INSERT INTO sr_knowledge_mastery(account_id,knowledge_card_id,system_suggested_level,evidence_count) VALUES(42,1,'L4',99)");
                    String[] ratings={"FORGOT","HARD","NORMAL","EASY"};
                    for(int i=0;i<ratings.length;i++) {
                        statement.executeUpdate("INSERT INTO sr_review_result(review_task_id,schedule_id,account_id,knowledge_card_id,recall_rating,previous_interval_days,next_interval_days,previous_next_review_at,calculated_next_review_at,created_at) VALUES("+(i+1)+",1,42,1,'"+ratings[i]+"',1,3,CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),DATE_ADD('2026-01-01',INTERVAL "+i+" DAY))");
                    }
                    statement.executeUpdate("INSERT INTO sr_review_result(review_task_id,schedule_id,account_id,knowledge_card_id,recall_rating,previous_interval_days,next_interval_days,previous_next_review_at,calculated_next_review_at) VALUES(5,2,42,999,'HARD',1,3,CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))");
                    connection.commit();
                    apply(connection,"V2_026__tutorial_chapter_lifecycle.sql");apply(connection,"V2_027__tutorial_learning_evidence_model.sql");
                    apply(connection,"V2_028__tutorial_learning_answer_versions.sql");apply(connection,"V2_029__tutorial_learning_plan_tasks.sql");apply(connection,"V2_030__tutorial_learning_legacy_orphan_evidence.sql");
                    try(ResultSet evidence=statement.executeQuery("SELECT rating FROM sr_learning_evidence WHERE knowledge_card_id=1 ORDER BY created_at,id")) {
                        for(String expected:new String[]{"FORGOT","FUZZY","REMEMBERED","REMEMBERED"}) { assertTrue(evidence.next());assertEquals(expected,evidence.getString(1)); }assertFalse(evidence.next());
                    }
                    try(ResultSet mastery=statement.executeQuery("SELECT evidence_count,recent_score,mastery_status FROM sr_knowledge_mastery WHERE account_id=42 AND knowledge_card_id=1")) {
                        assertTrue(mastery.next());assertEquals(4,mastery.getInt(1));assertEquals(5,mastery.getInt(2));assertEquals("BASIC_MASTERED",mastery.getString(3));
                    }
                    try(ResultSet answer=statement.executeQuery("SELECT v.answer_text,v.answer_phase,a.version_count FROM sr_user_question_answer a JOIN sr_user_question_answer_version v ON v.id=a.latest_version_id")) {
                        assertTrue(answer.next());assertEquals("Original answer",answer.getString(1));assertEquals("BEFORE_REFERENCE",answer.getString(2));assertEquals(1,answer.getInt(3));
                    }
                    try(ResultSet orphan=statement.executeQuery("SELECT rating,chapter_id FROM sr_learning_evidence WHERE knowledge_card_id=999")) { assertTrue(orphan.next());assertEquals("FUZZY",orphan.getString(1));assertNull(orphan.getObject(2)); }
                    assertCount(statement,"sr_review_result",5);assertCount(statement,"sr_learning_evidence",5);assertCount(statement,"sr_learning_legacy_completion",1);
                    }
                } finally {
                    connection.setCatalog(original);
                    if(created) admin.execute("DROP DATABASE "+schema);
                }
            }
        }
    }
    private void apply(Connection connection,String file) {
        ScriptUtils.executeSqlScript(connection,new EncodedResource(new ClassPathResource("db/migration/"+file),"UTF-8"));
    }
    private void assertCount(Statement statement,String table,int expected) throws Exception {
        assertTrue(java.util.Set.of("sr_review_result","sr_learning_evidence","sr_learning_legacy_completion").contains(table));
        try(ResultSet result=statement.executeQuery("SELECT COUNT(*) FROM "+table)) { assertTrue(result.next());assertEquals(expected,result.getInt(1)); }
    }
}
