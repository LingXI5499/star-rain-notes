package com.starrainnotes.english.shared.learning.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.shared.learning.dto.*;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EnglishLearningService {
    private static final Set<String> TYPES=Set.of("GRAMMAR","READING","LISTENING","WRITING");
    private static final Set<String> STATUSES=Set.of("NOT_STARTED","IN_PROGRESS","COMPLETED");
    private static final DateTimeFormatter ISO=DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final JdbcTemplate jdbc; private final ObjectMapper json; private final SiteSettingsTimezone timezone;
    public EnglishLearningService(JdbcTemplate jdbc,ObjectMapper json,SiteSettingsTimezone timezone){this.jdbc=jdbc;this.json=json;this.timezone=timezone;}

    @Transactional
    public LearningRecordView save(String learnerKey,String rawType,Long contentId,LearningRecordRequest request){
        String type=type(rawType); if(!STATUSES.contains(request.status()))bad("Unknown completion status.");
        Content content=content(type,contentId); long learner=learner(learnerKey);
        int seconds=request.timeSpentSeconds()==null?0:request.timeSpentSeconds();
        List<String> weak=normalizeWeak(request.weakPoints());
        String weakJson=write(weak); LocalDateTime next=nextReview(request.status(),request.mastery());
        jdbc.update("""
          INSERT INTO english_learning_record
          (learner_id,content_type,content_id,content_slug,cefr_level,completion_status,score,time_spent_seconds,
           attempts,weak_points_json,mastery_level,next_review_at,last_attempt_at)
          VALUES (?,?,?,?,?,?,?,?,1,?,?,?,UTC_TIMESTAMP(6))
          ON DUPLICATE KEY UPDATE content_slug=VALUES(content_slug),cefr_level=VALUES(cefr_level),
          completion_status=VALUES(completion_status),score=VALUES(score),
          time_spent_seconds=time_spent_seconds+VALUES(time_spent_seconds),attempts=attempts+1,
          weak_points_json=VALUES(weak_points_json),mastery_level=VALUES(mastery_level),
          next_review_at=VALUES(next_review_at),last_attempt_at=UTC_TIMESTAMP(6)
          """,learner,type,contentId,content.slug(),content.cefr(),request.status(),request.score(),seconds,
                weakJson,request.mastery(),next);
        jdbc.update("""
          INSERT INTO english_learning_attempt
          (learner_id,content_type,content_id,content_slug,cefr_level,completion_status,
           score,time_spent_seconds,weak_points_json,mastery_level)
          VALUES (?,?,?,?,?,?,?,?,?,?)
          """,learner,type,contentId,content.slug(),content.cefr(),request.status(),request.score(),seconds,
                weakJson,request.mastery());
        return getRequired(learner,type,contentId);
    }

    public LearningRecordView get(String learnerKey,String rawType,Long contentId){
        long learner=learner(learnerKey);String type=type(rawType);
        List<LearningRecordView> rows=jdbc.query(select()+" WHERE learner_id=? AND content_type=? AND content_id=?",
                this::map,learner,type,contentId);
        return rows.isEmpty()?null:rows.getFirst();
    }

    public LearningSummaryView summary(String learnerKey){
        long learner=learner(learnerKey);
        Long total=count("SELECT COUNT(*) FROM english_learning_record WHERE learner_id=?",learner);
        Long progress=count("SELECT COUNT(*) FROM english_learning_record WHERE learner_id=? AND completion_status='IN_PROGRESS'",learner);
        Long completed=count("SELECT COUNT(*) FROM english_learning_record WHERE learner_id=? AND completion_status='COMPLETED'",learner);
        Long due=count("SELECT COUNT(*) FROM english_learning_record WHERE learner_id=? AND next_review_at<=UTC_TIMESTAMP(6)",learner);
        Map<String,Long> byType=new LinkedHashMap<>();
        jdbc.query("SELECT content_type,COUNT(*) total FROM english_learning_record WHERE learner_id=? AND completion_status='COMPLETED' GROUP BY content_type",
                rs->{while(rs.next())byType.put(rs.getString(1),rs.getLong(2));return null;},learner);
        List<LearningRecordView> recent=jdbc.query(select()+" WHERE learner_id=? ORDER BY updated_at DESC LIMIT 8",this::map,learner);
        return new LearningSummaryView(orZero(total),orZero(progress),orZero(completed),orZero(due),byType,recent);
    }

    public LearningInsightsView insights(String learnerKey){
        long learner=learner(learnerKey);
        Map<String,Object> totals=jdbc.queryForMap("""
          SELECT COALESCE(SUM(time_spent_seconds),0) total_time,
                 COALESCE(SUM(attempts),0) total_attempts,
                 ROUND(AVG(score),2) average_score,
                 ROUND(AVG(mastery_level),4) average_mastery
          FROM english_learning_record WHERE learner_id=?
          """,learner);
        List<LearningActivityDayView> activity=activity(learner);
        return new LearningInsightsView(
                number(totals.get("total_time")),number(totals.get("total_attempts")),
                (int)activity.stream().filter(x->x.attempts()>0).count(),streak(activity),
                decimal(totals.get("average_score")),decimal(totals.get("average_mastery")),
                activity,modules(learner),recommendations(learner));
    }

    @Transactional
    public WritingSubmissionView saveSubmission(String learnerKey,Long promptId,WritingSubmissionRequest request){
        if(!Set.of("DRAFT","SUBMITTED").contains(request.status()))bad("Unknown submission status.");
        Integer exists=jdbc.queryForObject("SELECT COUNT(*) FROM english_writing_prompt WHERE id=? AND publish_status='PUBLISHED'",Integer.class,promptId);
        if(exists==null||exists==0)throw unavailable();
        long learner=learner(learnerKey);int words=countWords(request.bodyText());
        LocalDateTime submitted="SUBMITTED".equals(request.status())?LocalDateTime.now():null;
        jdbc.update("""
          INSERT INTO english_writing_submission(learner_id,prompt_id,body_text,word_count,submission_status,self_score,submitted_at)
          VALUES (?,?,?,?,?,?,?)
          ON DUPLICATE KEY UPDATE body_text=VALUES(body_text),word_count=VALUES(word_count),
          submission_status=VALUES(submission_status),self_score=VALUES(self_score),submitted_at=VALUES(submitted_at)
          """,learner,promptId,request.bodyText(),words,request.status(),request.selfScore(),submitted);
        return submission(learner,promptId);
    }

    public WritingSubmissionView getSubmission(String learnerKey,Long promptId){
        long learner=learner(learnerKey);
        try{return submission(learner,promptId);}catch(EmptyResultDataAccessException e){return null;}
    }

    private long learner(String key){
        if(key==null||key.length()<16||key.length()>128)throw new ApiException(HttpStatus.BAD_REQUEST,
                "ENGLISH_LEARNER_KEY_INVALID","Invalid learner key","A stable anonymous learner key is required.");
        String hash=sha256(key);
        jdbc.update("INSERT INTO english_learner_profile(learner_key_hash) VALUES (?) ON DUPLICATE KEY UPDATE last_seen_at=UTC_TIMESTAMP(6)",hash);
        return jdbc.queryForObject("SELECT id FROM english_learner_profile WHERE learner_key_hash=?",Long.class,hash);
    }
    private String type(String raw){String value=raw==null?"":raw.toUpperCase(Locale.ROOT);if(!TYPES.contains(value))bad("Unknown content type.");return value;}
    private Content content(String type,Long id){
        String sql=switch(type){
            case "GRAMMAR" -> "SELECT l.slug,NULL cefr FROM english_grammar_lesson l JOIN english_grammar_course c ON c.id=l.course_id WHERE l.id=? AND l.publish_status='PUBLISHED' AND c.publish_status='PUBLISHED'";
            case "READING" -> "SELECT slug,cefr_level cefr FROM english_reading_article WHERE id=? AND publish_status='PUBLISHED'";
            case "LISTENING" -> "SELECT slug,cefr_level cefr FROM english_listening_item WHERE id=? AND publish_status='PUBLISHED'";
            case "WRITING" -> "SELECT slug,cefr_level cefr FROM english_writing_prompt WHERE id=? AND publish_status='PUBLISHED'";
            default -> throw unavailable();
        };
        try{return jdbc.queryForObject(sql,(rs,n)->new Content(rs.getString("slug"),rs.getString("cefr")),id);}
        catch(EmptyResultDataAccessException e){throw unavailable();}
    }
    private LearningRecordView getRequired(long learner,String type,Long id){
        try{return jdbc.queryForObject(select()+" WHERE learner_id=? AND content_type=? AND content_id=?",this::map,learner,type,id);}
        catch(EmptyResultDataAccessException e){throw new ApiException(HttpStatus.NOT_FOUND,"ENGLISH_LEARNING_RECORD_NOT_FOUND","Record not found","No learning record exists.");}
    }
    private String select(){return "SELECT * FROM english_learning_record";}

    private List<LearningActivityDayView> activity(long learner){
        LocalDate today=LocalDate.now(timezone.zone());LocalDate first=today.minusDays(13);
        Map<LocalDate,int[]> grouped=new HashMap<>();
        jdbc.query("""
          SELECT attempted_at,completion_status,time_spent_seconds
          FROM english_learning_attempt
          WHERE learner_id=? AND attempted_at>=?
          ORDER BY attempted_at
          """,rs->{
            while(rs.next()){
                LocalDate day=timezone.atSite(rs.getTimestamp("attempted_at").toLocalDateTime()).toLocalDate();
                int[] value=grouped.computeIfAbsent(day,x->new int[3]);value[0]++;
                if("COMPLETED".equals(rs.getString("completion_status")))value[1]++;
                value[2]+=rs.getInt("time_spent_seconds");
            }return null;
        },learner,first.atStartOfDay(timezone.zone()).withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime());
        List<LearningActivityDayView> result=new ArrayList<>();
        for(int i=0;i<14;i++){LocalDate day=first.plusDays(i);int[] v=grouped.getOrDefault(day,new int[3]);result.add(new LearningActivityDayView(day.toString(),v[0],v[1],v[2]));}
        return result;
    }

    private int streak(List<LearningActivityDayView> activity){
        int index=activity.size()-1;if(index<0)return 0;
        if(activity.get(index).attempts()==0)index--;
        int value=0;for(;index>=0&&activity.get(index).attempts()>0;index--)value++;return value;
    }

    private List<LearningModuleInsightView> modules(long learner){
        Map<String,LearningModuleInsightView> values=new LinkedHashMap<>();
        for(String type:List.of("GRAMMAR","READING","LISTENING","WRITING"))
            values.put(type,new LearningModuleInsightView(type,0,0,null,0));
        jdbc.query("""
          SELECT content_type,COUNT(*) total,
                 SUM(completion_status='COMPLETED') completed,
                 ROUND(AVG(mastery_level),4) average_mastery,
                 SUM(time_spent_seconds) time_spent
          FROM english_learning_record WHERE learner_id=? GROUP BY content_type
          """,rs->{while(rs.next()){String type=rs.getString("content_type");values.put(type,
                    new LearningModuleInsightView(type,rs.getLong("total"),rs.getLong("completed"),
                            rs.getBigDecimal("average_mastery"),rs.getLong("time_spent")));}return null;},learner);
        return List.copyOf(values.values());
    }

    private List<LearningRecommendationView> recommendations(long learner){
        List<LearningRecordView> records=jdbc.query(select()+" "+"""
          WHERE learner_id=? AND (completion_status='IN_PROGRESS' OR next_review_at<=UTC_TIMESTAMP(6))
          ORDER BY (next_review_at<=UTC_TIMESTAMP(6)) DESC,next_review_at,mastery_level LIMIT 6
          """,this::map,learner);
        List<LearningRecommendationView> result=new ArrayList<>();
        for(LearningRecordView record:records){
            ContentLabel label=contentLabel(record.contentType(),record.contentId(),record.contentSlug());
            String reason=record.nextReviewAt()!=null?"到期复习":"继续学习";
            result.add(new LearningRecommendationView(record.contentType(),record.contentId(),record.contentSlug(),
                    label.title(),label.route(),reason,record.cefrLevel(),record.mastery(),record.nextReviewAt()));
        }
        if(result.isEmpty())result.addAll(starterRecommendations(learner));
        return result;
    }

    private List<LearningRecommendationView> starterRecommendations(long learner){
        List<LearningRecommendationView> result=new ArrayList<>();
        starter(result,learner,"GRAMMAR","""
          SELECT l.id,l.slug,l.title,NULL cefr FROM english_grammar_lesson l
          JOIN english_grammar_course c ON c.id=l.course_id
          WHERE l.publish_status='PUBLISHED' AND c.publish_status='PUBLISHED'
          AND NOT EXISTS(SELECT 1 FROM english_learning_record r WHERE r.learner_id=? AND r.content_type='GRAMMAR' AND r.content_id=l.id)
          ORDER BY l.sort_order,l.id LIMIT 1
          """);
        starter(result,learner,"READING","SELECT id,slug,title,cefr_level cefr FROM english_reading_article a WHERE publish_status='PUBLISHED' AND NOT EXISTS(SELECT 1 FROM english_learning_record r WHERE r.learner_id=? AND r.content_type='READING' AND r.content_id=a.id) ORDER BY sort_order,id LIMIT 1");
        starter(result,learner,"LISTENING","SELECT id,slug,title,cefr_level cefr FROM english_listening_item a WHERE publish_status='PUBLISHED' AND NOT EXISTS(SELECT 1 FROM english_learning_record r WHERE r.learner_id=? AND r.content_type='LISTENING' AND r.content_id=a.id) ORDER BY sort_order,id LIMIT 1");
        starter(result,learner,"WRITING","SELECT id,slug,title,cefr_level cefr FROM english_writing_prompt a WHERE publish_status='PUBLISHED' AND NOT EXISTS(SELECT 1 FROM english_learning_record r WHERE r.learner_id=? AND r.content_type='WRITING' AND r.content_id=a.id) ORDER BY sort_order,id LIMIT 1");
        return result;
    }

    private void starter(List<LearningRecommendationView> result,long learner,String type,String sql){
        jdbc.query(sql,rs->{if(rs.next())result.add(new LearningRecommendationView(type,rs.getLong("id"),
                rs.getString("slug"),rs.getString("title"),route(type,rs.getString("slug")),"建议开始",
                rs.getString("cefr"),null,null));return null;},learner);
    }

    private ContentLabel contentLabel(String type,Long id,String fallbackSlug){
        String sql=switch(type){
            case "GRAMMAR"->"SELECT title,slug FROM english_grammar_lesson WHERE id=?";
            case "READING"->"SELECT title,slug FROM english_reading_article WHERE id=?";
            case "LISTENING"->"SELECT title,slug FROM english_listening_item WHERE id=?";
            default->"SELECT title,slug FROM english_writing_prompt WHERE id=?";
        };
        List<ContentLabel> labels=jdbc.query(sql,(rs,n)->new ContentLabel(rs.getString("title"),route(type,rs.getString("slug"))),id);
        return labels.isEmpty()?new ContentLabel(fallbackSlug,route(type,fallbackSlug)):labels.getFirst();
    }

    private String route(String type,String slug){return switch(type){case"GRAMMAR"->"/english/grammar/"+slug;case"READING"->"/english/reading/"+slug;case"LISTENING"->"/english/listening/"+slug;default->"/english/writing/practice/"+slug;};}
    private LearningRecordView map(ResultSet r,int n)throws SQLException{return new LearningRecordView(r.getLong("id"),r.getString("content_type"),r.getLong("content_id"),r.getString("content_slug"),r.getString("cefr_level"),r.getString("completion_status"),r.getBigDecimal("score"),r.getInt("time_spent_seconds"),r.getInt("attempts"),readWeak(r.getString("weak_points_json")),r.getBigDecimal("mastery_level"),date(r,"next_review_at"),date(r,"updated_at"));}
    private WritingSubmissionView submission(long learner,Long prompt)throws EmptyResultDataAccessException{return jdbc.queryForObject("SELECT * FROM english_writing_submission WHERE learner_id=? AND prompt_id=?",(r,n)->new WritingSubmissionView(r.getLong("id"),r.getLong("prompt_id"),r.getString("body_text"),r.getInt("word_count"),r.getString("submission_status"),r.getBigDecimal("self_score"),date(r,"submitted_at"),date(r,"updated_at")),learner,prompt);}
    private LocalDateTime nextReview(String status,BigDecimal mastery){if(!"COMPLETED".equals(status))return null;double m=mastery==null?0:mastery.doubleValue();return LocalDateTime.now().plusDays(m>=.8?7:m>=.6?3:1);}
    private List<String> normalizeWeak(List<String> values){if(values==null)return List.of();return values.stream().filter(Objects::nonNull).map(String::trim).filter(x->!x.isBlank()).distinct().limit(20).toList();}
    private String write(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}}
    private List<String> readWeak(String value){try{return json.readValue(value,new TypeReference<>(){});}catch(Exception e){return List.of();}}
    private int countWords(String value){String v=value==null?"":value.trim();return v.isBlank()?0:v.split("\\s+").length;}
    private String sha256(String value){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));return HexFormat.of().formatHex(bytes);}catch(Exception e){throw new IllegalStateException(e);}}
    private Long count(String sql,Object... args){return jdbc.queryForObject(sql,Long.class,args);}private long orZero(Long v){return v==null?0:v;}
    private long number(Object value){return value instanceof Number n?n.longValue():0;}
    private BigDecimal decimal(Object value){return value instanceof BigDecimal d?d:value instanceof Number n?BigDecimal.valueOf(n.doubleValue()):null;}
    private String date(ResultSet r,String c)throws SQLException{var t=r.getTimestamp(c);return t==null?null:timezone.atSite(t.toLocalDateTime()).format(ISO);}
    private void bad(String detail){throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"ENGLISH_LEARNING_RECORD_INVALID","Invalid learning record",detail);}
    private ApiException unavailable(){return new ApiException(HttpStatus.NOT_FOUND,"ENGLISH_CONTENT_NOT_PUBLISHED","Content unavailable","The selected English content is not published.");}
    private record Content(String slug,String cefr){}
    private record ContentLabel(String title,String route){}
}
