package com.starrainnotes.english.vocabulary.family.service;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.english.vocabulary.family.dto.*;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class WordFamilyService {
 private static final DateTimeFormatter ISO=DateTimeFormatter.ISO_OFFSET_DATE_TIME;
 private final JdbcTemplate jdbc;private final SiteSettingsTimezone timezone;
 public WordFamilyService(JdbcTemplate jdbc,SiteSettingsTimezone timezone){this.jdbc=jdbc;this.timezone=timezone;}
 public List<WordFamilyView> list(String q){List<Object>a=new ArrayList<>();String w="";if(q!=null&&!q.isBlank()){w=" WHERE head_word LIKE ? OR slug LIKE ?";a.add("%"+q.trim()+"%");a.add("%"+q.trim()+"%");}return jdbc.query("SELECT id FROM vocabulary_word_family"+w+" ORDER BY head_word,id",a.toArray(),(r,n)->get(r.getLong(1)));}
 public WordFamilyView get(Long id){try{return jdbc.queryForObject("SELECT * FROM vocabulary_word_family WHERE id=?",(r,n)->new WordFamilyView(id,r.getString("head_word"),r.getString("slug"),r.getString("description"),links(id),members(id),timezone.atSite(r.getTimestamp("updated_at").toLocalDateTime()).format(ISO)),id);}catch(EmptyResultDataAccessException e){throw missing();}}
 public WordFamilyView publicGet(String slug){try{Long id=jdbc.queryForObject("SELECT id FROM vocabulary_word_family WHERE slug=?",Long.class,slug);return get(id);}catch(EmptyResultDataAccessException e){throw missing();}}
 @Transactional public WordFamilyView create(WordFamilyRequest r){String slug=NumericSlugGenerator.forCreate(r.slug(),this::slugExists);long id;try{jdbc.update("INSERT INTO vocabulary_word_family(head_word,slug,description) VALUES (?,?,?)",r.headWord().trim(),slug,clean(r.description()));id=jdbc.queryForObject("SELECT id FROM vocabulary_word_family WHERE slug=?",Long.class,slug);}catch(DuplicateKeyException e){throw conflict();}replaceLinks(id,r.wordIds());return get(id);}
 @Transactional public WordFamilyView update(Long id,WordFamilyRequest r){WordFamilyView current=get(id);String slug=NumericSlugGenerator.forUpdate(r.slug(),current.slug());try{jdbc.update("UPDATE vocabulary_word_family SET head_word=?,slug=?,description=? WHERE id=?",r.headWord().trim(),slug,clean(r.description()),id);}catch(DuplicateKeyException e){throw conflict();}replaceLinks(id,r.wordIds());return get(id);}
 @Transactional public void delete(Long id){get(id);jdbc.update("DELETE FROM vocabulary_word_family WHERE id=?",id);}
 @Transactional public WordFamilyMemberView addMember(Long family,WordFamilyMemberRequest r){get(family);validate(r);int order=r.sortOrder()==null?next(family):r.sortOrder();jdbc.update("INSERT INTO vocabulary_family_member(family_id,spelling,part_of_speech,phonetic_us,translation,cefr_level,example_sentence,example_translation,sort_order) VALUES (?,?,?,?,?,?,?,?,?)",family,r.spelling().trim(),clean(r.partOfSpeech()),clean(r.phoneticUs()),clean(r.translation()),clean(r.cefrLevel()),clean(r.exampleSentence()),clean(r.exampleTranslation()),order);Long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);return member(id);}
 @Transactional public WordFamilyMemberView updateMember(Long family,Long id,WordFamilyMemberRequest r){owned(family,id);validate(r);jdbc.update("UPDATE vocabulary_family_member SET spelling=?,part_of_speech=?,phonetic_us=?,translation=?,cefr_level=?,example_sentence=?,example_translation=?,sort_order=COALESCE(?,sort_order) WHERE id=?",r.spelling().trim(),clean(r.partOfSpeech()),clean(r.phoneticUs()),clean(r.translation()),clean(r.cefrLevel()),clean(r.exampleSentence()),clean(r.exampleTranslation()),r.sortOrder(),id);return member(id);}
 @Transactional public void deleteMember(Long family,Long id){owned(family,id);jdbc.update("DELETE FROM vocabulary_family_member WHERE id=?",id);}
 @Transactional public void move(Long family,Long id,int target){owned(family,id);List<Long>ids=jdbc.queryForList("SELECT id FROM vocabulary_family_member WHERE family_id=? ORDER BY sort_order,id",Long.class,family);ids.remove(id);ids.add(Math.min(Math.max(target,0),ids.size()),id);for(int i=0;i<ids.size();i++)jdbc.update("UPDATE vocabulary_family_member SET sort_order=? WHERE id=?",(i+1)*10,ids.get(i));}
 private void replaceLinks(Long family,List<Long>ids){jdbc.update("DELETE FROM vocabulary_word_family_link WHERE family_id=?",family);for(Long id:new LinkedHashSet<>(ids==null?List.of():ids)){Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM vocabulary_word WHERE id=?",Integer.class,id);if(n==null||n==0)bad("Linked vocabulary word does not exist.");jdbc.update("INSERT INTO vocabulary_word_family_link(family_id,word_id) VALUES (?,?)",family,id);}}
 private List<Long>links(Long id){return jdbc.queryForList("SELECT word_id FROM vocabulary_word_family_link WHERE family_id=? ORDER BY word_id",Long.class,id);}
 private List<WordFamilyMemberView>members(Long id){return jdbc.query("SELECT * FROM vocabulary_family_member WHERE family_id=? ORDER BY sort_order,id",(r,n)->map(r),id);}
 private WordFamilyMemberView member(Long id){try{return jdbc.queryForObject("SELECT * FROM vocabulary_family_member WHERE id=?",(r,n)->map(r),id);}catch(EmptyResultDataAccessException e){throw memberMissing();}}
 private WordFamilyMemberView map(java.sql.ResultSet r)throws java.sql.SQLException{return new WordFamilyMemberView(r.getLong("id"),r.getString("spelling"),r.getString("part_of_speech"),r.getString("phonetic_us"),r.getString("translation"),r.getString("cefr_level"),r.getString("example_sentence"),r.getString("example_translation"),r.getInt("sort_order"));}
 private void validate(WordFamilyMemberRequest r){String c=clean(r.cefrLevel());if(c!=null){Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM english_cefr_standard WHERE level=?",Integer.class,c);if(n==null||n==0)bad("Invalid CEFR level.");}if(r.sortOrder()!=null&&r.sortOrder()<0)bad("Invalid sort order.");}
 private boolean slugExists(String slug){Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM vocabulary_word_family WHERE slug=?",Integer.class,slug);return n!=null&&n>0;}
 private int next(Long family){Integer x=jdbc.queryForObject("SELECT COALESCE(MAX(sort_order),0) FROM vocabulary_family_member WHERE family_id=?",Integer.class,family);return(x==null?0:x)+10;}private void owned(Long f,Long id){Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM vocabulary_family_member WHERE family_id=? AND id=?",Integer.class,f,id);if(n==null||n==0)throw memberMissing();}
 private String clean(String s){return s==null||s.isBlank()?null:s.trim();}private ApiException missing(){return new ApiException(HttpStatus.NOT_FOUND,"VOCABULARY_WORD_FAMILY_NOT_FOUND","Word family not found","The word family does not exist.");}private ApiException memberMissing(){return new ApiException(HttpStatus.NOT_FOUND,"VOCABULARY_FAMILY_MEMBER_NOT_FOUND","Family member not found","The word-family member does not exist.");}private ApiException conflict(){return new ApiException(HttpStatus.CONFLICT,"VOCABULARY_WORD_FAMILY_SLUG_CONFLICT","Slug already exists","Choose another family slug.");}private void bad(String d){throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"VOCABULARY_WORD_FAMILY_INVALID","Invalid word family",d);}
}
