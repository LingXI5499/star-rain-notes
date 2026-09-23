package com.starrainnotes.english.learning.infrastructure;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.learning.dto.LearningRecordView;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class LearningRecordRepository {
    private final JdbcTemplate jdbc;
    private final LearningRecordViewMapper mapper;
    public LearningRecordRepository(JdbcTemplate jdbc, LearningRecordViewMapper mapper) {
        this.jdbc=jdbc;
        this.mapper=mapper;
    }
    public List<Map<String,Object>> list(long profileId,int page,int pageSize) {
        return jdbc.queryForList("""
                SELECT id,content_type,content_id,content_slug,cefr_level,completion_status,score,
                       time_spent_seconds,attempts,mastery_level,next_review_at,updated_at
                FROM english_learning_record WHERE learner_id=? ORDER BY updated_at DESC LIMIT ? OFFSET ?
                """,profileId,Math.min(Math.max(pageSize,1),50),(Math.max(page,1)-1)*50);
    }
    public Map<String,Object> get(long profileId,String type,Long contentId) {
        try { return jdbc.queryForMap("""
                SELECT id,content_type,content_id,content_slug,cefr_level,completion_status,score,
                       time_spent_seconds,attempts,mastery_level,next_review_at,updated_at
                FROM english_learning_record WHERE learner_id=? AND content_type=? AND content_id=?
                """,profileId,type,contentId); }
        catch(EmptyResultDataAccessException ex) { throw missing(); }
    }
    public void saveSimple(long profileId,String type,Long contentId,String status,Integer seconds) {
        int time=seconds==null?0:seconds;
        int updated=jdbc.update("""
                UPDATE english_learning_record SET completion_status=?,time_spent_seconds=?,attempts=attempts+1,last_attempt_at=UTC_TIMESTAMP(6)
                WHERE learner_id=? AND content_type=? AND content_id=?
                """,status,time,profileId,type,contentId);
        if(updated==0) jdbc.update("""
                INSERT INTO english_learning_record(learner_id,content_type,content_id,content_slug,completion_status,time_spent_seconds,attempts,weak_points_json)
                VALUES (?,?,?,?,?,?,1,JSON_ARRAY())
                """,profileId,type,contentId,type.toLowerCase()+"-"+contentId,status,time);
    }
    public Map<String,LearningRecordView> batch(long learnerId, List<ContentRef> refs) {
        if (refs.isEmpty()) return Map.of();
        StringBuilder where = new StringBuilder();
        List<Object> args = new ArrayList<>();
        args.add(learnerId);
        for (ContentRef ref : refs) {
            if (!where.isEmpty()) where.append(" OR ");
            where.append("(content_type=? AND content_id=?)");
            args.add(ref.type());
            args.add(ref.id());
        }
        LinkedHashMap<String,LearningRecordView> result = new LinkedHashMap<>();
        jdbc.query("SELECT * FROM english_learning_record WHERE learner_id=? AND (" + where
                + ") ORDER BY updated_at DESC", rs -> {
            while (rs.next()) {
                LearningRecordView value = mapper.map(rs, 0);
                result.put(value.contentType() + ":" + value.contentId(), value);
            }
            return null;
        }, args.toArray());
        return result;
    }
    public List<LearningRecordView> recent(long learnerId) {
        return jdbc.query("SELECT * FROM english_learning_record WHERE learner_id=? ORDER BY updated_at DESC LIMIT 8",
                mapper::map, learnerId);
    }
    public record ContentRef(String type, long id) {}
    private ApiException missing() { return new ApiException(HttpStatus.NOT_FOUND,"LEARNING_RECORD_NOT_FOUND","Not found","The requested data does not exist."); }
}
