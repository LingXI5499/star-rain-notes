package com.starrainnotes.boot;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.knowledge.dto.RevisionDto;
import com.starrainnotes.english.knowledge.mapper.*;
import com.starrainnotes.english.knowledge.service.impl.*;
import com.starrainnotes.english.knowledge.utils.EnglishBodyValidator;
import com.starrainnotes.english.taxonomy.mapper.TaxonomyMapper;
import com.starrainnotes.english.taxonomy.service.impl.TaxonomyServiceImpl;
import com.starrainnotes.english.reading.dto.ReadingDto;
import com.starrainnotes.english.reading.dto.ReadingEnhancementDto.*;
import com.starrainnotes.english.reading.mapper.*;
import com.starrainnotes.english.reading.service.impl.*;
import com.starrainnotes.english.writing.article.dto.WritingArticleDto;
import com.starrainnotes.english.writing.article.mapper.WritingArticleMapper;
import com.starrainnotes.english.writing.article.service.impl.WritingArticleServiceImpl;
import java.sql.*;
import java.util.*;
import org.apache.ibatis.session.SqlSession;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
/** Real Flyway upgrade and service/SQL contracts in one disposable MySQL schema. */
class EnglishRwIntegrationTest extends MapperXmlIntegrationSupport {
 @Test void flywayUpgradeAndReadingWritingContracts() throws Exception {
  try(SqlSession session=openSession()) {
   Connection connection=session.getConnection();String original=connection.getCatalog();
   String schema="sr_english_rw_probe_"+Long.toUnsignedString(System.nanoTime());
   assertTrue(schema.matches("sr_english_rw_probe_[0-9]+"));boolean created=false;
   try(Statement admin=connection.createStatement()) {
    try {
     admin.execute("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4");created=true;connection.setCatalog(schema);
     SingleConnectionDataSource ds=new SingleConnectionDataSource(connection,true);
     Flyway.configure().dataSource(ds).defaultSchema(schema).schemas(schema).target("2.034").load().migrate();
     try(Statement sql=connection.createStatement()) {sql.executeUpdate("INSERT INTO sr_english_reading_article(slug,title,summary,body_markdown,reading_level,cefr_level,publish_status) VALUES('legacy-probe','Legacy','','Legacy English',1,'A1','PUBLISHED')");}
     Flyway flyway=Flyway.configure().dataSource(ds).defaultSchema(schema).schemas(schema).load();flyway.migrate();
     assertEquals("2.037",flyway.info().current().getVersion().getVersion());
     try(Statement sql=connection.createStatement();ResultSet result=sql.executeQuery("SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('2.035','2.036') AND success=1")) {assertTrue(result.next());assertEquals(2,result.getInt(1));}
     var taxonomy=new TaxonomyServiceImpl(session.getMapper(TaxonomyMapper.class));
     var roots=taxonomy.tree(false);assertEquals(16,roots.stream().filter(t->"TOPIC".equals(t.getDimension())).count());
     assertEquals(144,roots.stream().filter(t->"TOPIC".equals(t.getDimension())).mapToInt(t->t.getChildren().size()).sum());
     assertEquals(9,roots.stream().filter(t->"GENRE".equals(t.getDimension())).count());
     assertEquals(74,roots.stream().filter(t->"GENRE".equals(t.getDimension())).mapToInt(t->t.getChildren().size()).sum());
     assertEquals(18,roots.stream().filter(t->"PURPOSE".equals(t.getDimension())).count());
     Long topic=roots.stream().filter(t->"TOPIC".equals(t.getDimension())).findFirst().orElseThrow().getId();
     Long child=roots.stream().filter(t->"TOPIC".equals(t.getDimension())).findFirst().orElseThrow().getChildren().getFirst().getId();
     ObjectMapper json=new ObjectMapper().findAndRegisterModules();
     var metadata=new DocumentMetadataServiceImpl(session.getMapper(DocumentMetadataMapper.class),taxonomy,json);
     CurrentActorApi actors=mock(CurrentActorApi.class);
     var adminActor=CurrentActorApi.CurrentActor.builder().accountId(71L).roles(Set.of("SUPER_ADMIN")).permissions(Set.of("english:content-publish")).build();
     when(actors.current()).thenReturn(adminActor);
     ReadingMapper readMapper=session.getMapper(ReadingMapper.class);ReadingEnhancementMapper enhanceMapper=session.getMapper(ReadingEnhancementMapper.class);
     var reading=new ReadingServiceImpl(readMapper,metadata,taxonomy,enhanceMapper);
     var enhancements=new ReadingEnhancementServiceImpl(reading,readMapper,enhanceMapper);
     var revisions=new ReadingRevisionServiceImpl(reading,readMapper,enhancements,metadata,actors,json);
     assertNull(readMapper.publicBySlug("legacy-probe"));
     assertEquals("PENDING",readMapper.rights(readMapper.list(false,"Legacy",List.of(),List.of(),List.of(),0,5).getFirst().getId()).getRightsStatus());
     var r=ReadingDto.Request.builder().bodyMarkdown("").contentOrigin("ORIGINAL").build();r.setPrimaryTopicId(child);r.setOtherTopicIds(List.of(child,topic));
     var article=reading.create(r);String id=article.getId().toString();assertEquals("Untitled Article",article.getTitle());
     var jsonArticle=json.valueToTree(article);
     for(String removed:List.of("cefrLevel","difficultyLevel","levelAssessed"))assertFalse(jsonArticle.has(removed));
     assertThrows(ApiException.class,()->reading.setPublished(id,true,0L));
     r.setBodyMarkdown("English **body**.");r.setTranslationZhMarkdown("中文正文。");r.setRowVersion(0L);article=reading.update(id,r);
     article=reading.setPublished(id,true,article.getRowVersion());assertEquals(1,reading.listFiltered(false,"",1,12,topic,null,null).getTotal());
     assertThrows(ApiException.class,()->reading.update(id,r));
     Item en=anchor("EN","English body.",article.getBodyMarkdown());Item zh=anchor("ZH","中文正文。",article.getTranslationZhMarkdown());
     Batch batch=new Batch();batch.setRowVersion(article.getRowVersion());batch.setItems(List.of(en,zh));var aligned=enhancements.replace(id,"alignments",batch);
     assertEquals(2,enhancements.list(article.getSlug(),false,"alignments").size());
     Item noun=new Item();noun.setWord("record");noun.setPartOfSpeech("NOUN");noun.setMeaningZh("记录");noun.setRowVersion(aligned.rowVersion());enhancements.save(id,"vocabulary",null,noun);
     Item verb=new Item();verb.setWord("record");verb.setPartOfSpeech("VERB");verb.setMeaningZh("记录下来");verb.setRowVersion(reading.get(id,true).getRowVersion());enhancements.save(id,"vocabulary",null,verb);
     assertEquals(2,enhancements.list(article.getSlug(),false,"vocabulary").size());
     Item note=anchor("EN","English body.",article.getBodyMarkdown());note.setAnalysisMarkdown("人工解析");note.setRowVersion(reading.get(id,true).getRowVersion());enhancements.save(id,"annotations",null,note);
     RevisionDto.Request rev=request(reading.get(id,true).getRowVersion());assertEquals(1,revisions.snapshot(id,rev).getRevisionNo());
     r.setRowVersion(reading.get(id,true).getRowVersion());r.setBodyMarkdown("Changed English.");article=reading.update(id,r);
     assertTrue(enhancements.list(article.getSlug(),false,"alignments").isEmpty());assertTrue(enhancements.list(article.getSlug(),false,"annotations").isEmpty());
     assertEquals("STALE",enhancements.list(id,true,"annotations").getFirst().getStatus());
     rev=request(article.getRowVersion());assertEquals(2,revisions.snapshot(id,rev).getRevisionNo());article=revisions.restore(id,1,rev);
     assertEquals("English **body**.",article.getBodyMarkdown());assertEquals(3,revisions.list(id,1,12).total());assertEquals(2,enhancements.list(article.getSlug(),false,"alignments").size());
     // Rights enforcement, including an external article with empty metadata.
     var ext=ReadingDto.Request.builder().bodyMarkdown("External article.").contentOrigin("EXTERNAL").build();
     var external=reading.create(ext);String externalId=external.getId().toString();assertThrows(ApiException.class,()->reading.setPublished(externalId,true,0L));
     ReadingDto.Rights rights=new ReadingDto.Rights();rights.setRightsStatus("CLEARED");rights.setRightsBasis("test fixture permission");ext.setRights(rights);ext.setRowVersion(0L);external=reading.update(externalId,ext);reading.setPublished(externalId,true,external.getRowVersion());
     var writing=new WritingArticleServiceImpl(session.getMapper(WritingArticleMapper.class),metadata,taxonomy,actors,json);
     WritingArticleDto.Request w=new WritingArticleDto.Request();w.setBodyMarkdown("");w.setPrimaryTopicId(child);w.setKeywords(List.of("one","two"));
     var owned=writing.create(w);String wid=owned.getId().toString();assertEquals("PRIVATE",owned.getVisibility());assertThrows(ApiException.class,()->writing.complete(wid,0L));
     w.setBodyMarkdown("My original.");w.setRowVersion(0L);owned=writing.save(wid,w);assertThrows(ApiException.class,()->writing.save(wid,w));
     owned=writing.complete(wid,owned.getRowVersion());owned=writing.publish(wid,owned.getRowVersion(),true);assertNotNull(writing.getPublic(owned.getSlug()));
     assertEquals(3,writing.snapshot(wid,request(owned.getRowVersion())).getRevisionNo());w.setBodyMarkdown("Second version.");w.setRowVersion(owned.getRowVersion());owned=writing.save(wid,w);
     assertEquals(4,writing.snapshot(wid,request(owned.getRowVersion())).getRevisionNo());owned=writing.restore(wid,3,request(owned.getRowVersion()));assertEquals("My original.",owned.getBodyMarkdown());assertEquals(List.of("one","two"),owned.getKeywords());assertEquals(5,writing.revisions(wid,1,12).total());
     var other=CurrentActorApi.CurrentActor.builder().accountId(72L).roles(Set.of("USER")).permissions(Set.of()).build();when(actors.current()).thenReturn(other);
     assertThrows(ApiException.class,()->writing.get(wid));assertThrows(ApiException.class,()->writing.save(wid,w));assertThrows(ApiException.class,()->writing.revisions(wid,1,12));assertThrows(ApiException.class,()->writing.restore(wid,1,request(0L)));assertThrows(ApiException.class,()->writing.delete(wid));
     w.setBodyMarkdown("Private work.");w.setRowVersion(null);var privateArticle=writing.create(w);var completed=writing.complete(privateArticle.getId().toString(),0L);assertThrows(ApiException.class,()->writing.publish(privateArticle.getId().toString(),completed.getRowVersion(),true));
     assertNull(session.getMapper(WritingArticleMapper.class).publicBySlug(privateArticle.getSlug()));
     var knowledge=new KnowledgeServiceImpl(session.getMapper(KnowledgeMapper.class),taxonomy);
     assertEquals(2,knowledge.list(null,"",topic,null,null,1,12).total());assertEquals(1,knowledge.list("WRITING","",topic,null,null,1,12).total());
     assertEquals(1,knowledge.list(null,"",topic,null,null,1,1).items().size());assertNotEquals(knowledge.list(null,"",topic,null,null,1,1).items().getFirst().getType(),knowledge.list(null,"",topic,null,null,2,1).items().getFirst().getType());
     taxonomy.disable(child);
     assertThrows(ApiException.class,()->taxonomy.validateIds(List.of(child),"TOPIC"));
     var historical=writing.restore(privateArticle.getId().toString(),1,request(completed.getRowVersion()));
     assertEquals(child,historical.getPrimaryTopicId(),"停用分类仍保留历史引用");
     when(actors.current()).thenReturn(adminActor);
     var historicalReading=revisions.restore(id,1,request(reading.get(id,true).getRowVersion()));
     assertEquals(child,historicalReading.getPrimaryTopicId());
     session.rollback();
    } finally { connection.setCatalog(original);if(created)admin.execute("DROP DATABASE "+schema); }
   }
  }
 }
 private static RevisionDto.Request request(Long version) {var r=new RevisionDto.Request();r.setRowVersion(version);return r;}
 private static Item anchor(String language,String expected,String source) { Item i=new Item();i.setLanguage(language);i.setGroupKey("group-one");i.setParagraphIndex(0);i.setRangeStart(0);i.setRangeEnd(expected.length());i.setExpectedText(expected);i.setParagraphHash(EnglishBodyValidator.hash(expected));i.setSourceMarkdownHash(EnglishBodyValidator.hash(source));return i; }
}
