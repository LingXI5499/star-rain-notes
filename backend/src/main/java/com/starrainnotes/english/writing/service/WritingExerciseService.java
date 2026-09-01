package com.starrainnotes.english.writing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.reading.dto.ReadingCheckAnswerRequest;
import com.starrainnotes.english.reading.dto.ReadingCheckItemView;
import com.starrainnotes.english.reading.dto.ReadingCheckResultView;
import com.starrainnotes.english.reading.dto.ReadingExercisePublicView;
import com.starrainnotes.english.reading.dto.ReadingExerciseRequest;
import com.starrainnotes.english.reading.dto.ReadingExerciseView;
import com.starrainnotes.english.shared.exercise.entity.EnglishExercise;
import com.starrainnotes.english.shared.exercise.mapper.EnglishExerciseMapper;
import com.starrainnotes.english.shared.exercise.service.EnglishExerciseSafety;
import com.starrainnotes.english.shared.exercise.service.EnglishExerciseService;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

/** Writing task exercises. Answers are never included in the public view. */
@Service
public class WritingExerciseService {
    private static final String MODULE = "WRITING";
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final JdbcTemplate jdbc; private final EnglishExerciseMapper mapper; private final EnglishExerciseService rules;
    private final EnglishExerciseSafety safety; private final WritingPromptService prompts; private final ObjectMapper json; private final SiteSettingsTimezone timezone;
    public WritingExerciseService(JdbcTemplate jdbc, EnglishExerciseMapper mapper, EnglishExerciseService rules, EnglishExerciseSafety safety, WritingPromptService prompts, ObjectMapper json, SiteSettingsTimezone timezone){this.jdbc=jdbc;this.mapper=mapper;this.rules=rules;this.safety=safety;this.prompts=prompts;this.json=json;this.timezone=timezone;}
    public List<ReadingExerciseView> list(Long promptId){prompts.get(promptId);return jdbc.query(sql("WHERE pe.prompt_id=? ORDER BY e.sort_order,e.id"),(rs,n)->admin(rs,promptId),promptId);}
    @Transactional public ReadingExerciseView create(Long promptId,ReadingExerciseRequest request){prompts.get(promptId);rules.validateConfig(MODULE,request.questionType(),request.configJson());EnglishExercise e=new EnglishExercise();e.setModuleType(MODULE);e.setQuestionType(request.questionType());e.setPromptMarkdown(request.promptMarkdown());e.setConfigJson(parse(request.configJson()));e.setExplanationMarkdown(clean(request.explanationMarkdown()));e.setScoreValue(requireScore(request.scoreValue()));e.setSortOrder(next(promptId));e.setPublishStatus(status(request.publishStatus()));mapper.insert(e);jdbc.update("INSERT INTO english_writing_prompt_exercise(prompt_id,exercise_id) VALUES (?,?)",promptId,e.getId());return get(promptId,e.getId());}
    @Transactional public ReadingExerciseView update(Long promptId,Long exerciseId,ReadingExerciseRequest request){binding(promptId,exerciseId);rules.validateConfig(MODULE,request.questionType(),request.configJson());EnglishExercise e=exercise(exerciseId);e.setQuestionType(request.questionType());e.setPromptMarkdown(request.promptMarkdown());e.setConfigJson(parse(request.configJson()));e.setExplanationMarkdown(clean(request.explanationMarkdown()));e.setScoreValue(requireScore(request.scoreValue()));if(request.publishStatus()!=null)e.setPublishStatus(status(request.publishStatus()));mapper.updateById(e);return get(promptId,exerciseId);}
    @Transactional public void move(Long promptId,Long exerciseId,int index){binding(promptId,exerciseId);List<Long>ids=new ArrayList<>(jdbc.queryForList("SELECT e.id FROM english_writing_prompt_exercise pe JOIN english_exercise e ON e.id=pe.exercise_id WHERE pe.prompt_id=? ORDER BY e.sort_order,e.id",Long.class,promptId));ids.remove(exerciseId);ids.add(Math.min(Math.max(0,index),ids.size()),exerciseId);for(int i=0;i<ids.size();i++)jdbc.update("UPDATE english_exercise SET sort_order=? WHERE id=?",(i+1)*10,ids.get(i));}
    @Transactional public void delete(Long promptId,Long exerciseId){binding(promptId,exerciseId);jdbc.update("DELETE FROM english_writing_prompt_exercise WHERE prompt_id=? AND exercise_id=?",promptId,exerciseId);mapper.deleteById(exerciseId);}
    public List<ReadingExercisePublicView> publicList(String slug){Long id=publishedPrompt(slug);return jdbc.query(sql("WHERE pe.prompt_id=? AND e.publish_status='PUBLISHED' ORDER BY e.sort_order,e.id"),(rs,n)->publicView(rs),id);}
    @Transactional public ReadingCheckResultView check(String slug,ReadingCheckAnswerRequest request){Long promptId=publishedPrompt(slug);int score=0,total=0;List<ReadingCheckItemView>items=new ArrayList<>();Set<Long>unique=new HashSet<>();for(var sub:request.answers()){if(!unique.add(sub.exerciseId()))bad("The same exercise cannot be submitted twice.");EnglishExercise e=publishedBinding(promptId,sub.exerciseId());boolean ok=safety.isCorrect(e.getQuestionType(),json.valueToTree(e.getConfigJson()),sub.answer());int earned=ok?e.getScoreValue():0;score+=earned;total+=e.getScoreValue();items.add(new ReadingCheckItemView(e.getId(),ok,earned,e.getScoreValue(),e.getExplanationMarkdown()));}return new ReadingCheckResultView(score,total,items);}
    private String sql(String suffix){return "SELECT e.id,e.question_type,e.prompt_markdown,e.config_json,e.explanation_markdown,e.score_value,e.sort_order,e.publish_status,e.updated_at FROM english_writing_prompt_exercise pe JOIN english_exercise e ON e.id=pe.exercise_id "+suffix;}
    private ReadingExerciseView get(Long promptId,Long exerciseId){return jdbc.queryForObject(sql("WHERE e.id=?"),(rs,n)->admin(rs,promptId),exerciseId);}
    private ReadingExerciseView admin(java.sql.ResultSet rs,Long promptId)throws java.sql.SQLException{return new ReadingExerciseView(rs.getLong("id"),promptId,rs.getString("question_type"),rs.getString("prompt_markdown"),json.convertValue(tree(rs.getString("config_json")),new TypeReference<>(){}),rs.getString("explanation_markdown"),rs.getInt("score_value"),rs.getInt("sort_order"),rs.getString("publish_status"),date(rs.getTimestamp("updated_at")));}
    private ReadingExercisePublicView publicView(java.sql.ResultSet rs)throws java.sql.SQLException{long id=rs.getLong("id");String type=rs.getString("question_type");return new ReadingExercisePublicView(id,type,rs.getString("prompt_markdown"),safety.sanitize(type,tree(rs.getString("config_json")),id),rs.getInt("score_value"),rs.getInt("sort_order"));}
    private Map<String,Object> parse(String raw){try{return json.readValue(raw,new TypeReference<>(){});}catch(Exception e){throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"ENGLISH_EXERCISE_CONFIG_INVALID","Invalid exercise config","config_json is not valid JSON.");}}
    private JsonNode tree(String raw){try{return json.readTree(raw==null?"{}":raw);}catch(Exception e){return json.createObjectNode();}}
    private void binding(Long promptId,Long exerciseId){Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM english_writing_prompt_exercise WHERE prompt_id=? AND exercise_id=?",Integer.class,promptId,exerciseId);if(n==null||n==0)throw new ApiException(HttpStatus.NOT_FOUND,"ENGLISH_WRITING_EXERCISE_NOT_FOUND","Exercise not found","The exercise is not bound to this writing task.");}
    private EnglishExercise exercise(Long id){EnglishExercise e=mapper.selectById(id);if(e==null||!MODULE.equals(e.getModuleType()))throw new ApiException(HttpStatus.NOT_FOUND,"ENGLISH_WRITING_EXERCISE_NOT_FOUND","Exercise not found","The exercise does not exist.");return e;}
    private EnglishExercise publishedBinding(Long promptId,Long exerciseId){List<Long>ids=jdbc.queryForList("SELECT e.id FROM english_writing_prompt_exercise pe JOIN english_exercise e ON e.id=pe.exercise_id WHERE pe.prompt_id=? AND e.id=? AND e.module_type='WRITING' AND e.publish_status='PUBLISHED'",Long.class,promptId,exerciseId);if(ids.isEmpty()){bad("The submitted exercise is not published for this writing task.");}return exercise(ids.get(0));}
    private Long publishedPrompt(String slug){Long id=jdbc.query("SELECT id FROM english_writing_prompt WHERE slug=? AND publish_status='PUBLISHED'",rs->rs.next()?rs.getLong(1):null,slug);if(id==null)throw new ApiException(HttpStatus.NOT_FOUND,"ENGLISH_CONTENT_NOT_PUBLISHED","Content not available","The writing task is not published.");return id;}
    private int next(Long promptId){Integer n=jdbc.queryForObject("SELECT COALESCE(MAX(e.sort_order),0) FROM english_writing_prompt_exercise pe JOIN english_exercise e ON e.id=pe.exercise_id WHERE pe.prompt_id=?",Integer.class,promptId);return(n==null?0:n)+10;}
    private int requireScore(Integer v){if(v==null||v<=0||v>100)bad("scoreValue must be between 1 and 100.");return v;}private String status(String v){if(v==null)return "DRAFT";if(!v.equals("DRAFT")&&!v.equals("PUBLISHED"))bad("Invalid exercise status.");return v;}private void bad(String s){throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"ENGLISH_WRITING_ANSWER_INVALID","Invalid writing request",s);}private String clean(String s){return s==null||s.isBlank()?null:s.trim();}private String date(java.sql.Timestamp t){return t==null?null:timezone.atSite(t.toLocalDateTime()).format(ISO);}
}
