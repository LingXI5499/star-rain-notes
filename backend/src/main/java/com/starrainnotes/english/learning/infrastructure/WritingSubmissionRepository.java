package com.starrainnotes.english.learning.infrastructure;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.learning.dto.WritingSubmissionRequest;
import com.starrainnotes.english.learning.dto.WritingSubmissionView;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Repository
public class WritingSubmissionRepository {
    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;
    public WritingSubmissionRepository(JdbcTemplate jdbc, SiteSettingsTimezone timezone) {
        this.jdbc=jdbc; this.timezone=timezone;
    }
    public Map<String,Object> get(long learnerId,long promptId) {
        try { return jdbc.queryForMap("SELECT * FROM english_writing_submission WHERE learner_id=? AND prompt_id=? LIMIT 1",learnerId,promptId); }
        catch(EmptyResultDataAccessException ex) { throw new ApiException(HttpStatus.NOT_FOUND,"WRITING_SUBMISSION_NOT_FOUND","Not found","The requested data does not exist."); }
    }
    public WritingSubmissionView getView(long learnerId, long promptId) {
        try {
            return jdbc.queryForObject("""
                    SELECT * FROM english_writing_submission
                    WHERE learner_id=? AND prompt_id=? LIMIT 1
                    """, this::view, learnerId, promptId);
        } catch (EmptyResultDataAccessException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "WRITING_SUBMISSION_NOT_FOUND",
                    "Not found", "The requested data does not exist.");
        }
    }
    public void requirePublishedPrompt(long promptId) {
        Integer exists=jdbc.queryForObject("SELECT COUNT(*) FROM english_writing_prompt WHERE id=? AND publish_status='PUBLISHED'",Integer.class,promptId);
        if(exists==null||exists==0) throw new ApiException(HttpStatus.NOT_FOUND,"ENGLISH_CONTENT_NOT_PUBLISHED","Content unavailable","The selected English content is not published.");
    }
    public WritingSubmissionView save(long learnerId,long promptId,WritingSubmissionRequest request) {
        String body=request.bodyText();
        String trimmed=body==null?"":body.trim();
        int words=trimmed.isBlank()?0:trimmed.split("\\s+").length;
        LocalDateTime submitted="SUBMITTED".equals(request.status())?LocalDateTime.now(ZoneOffset.UTC):null;
        jdbc.update("""
                INSERT INTO english_writing_submission(learner_id,prompt_id,body_text,word_count,submission_status,self_score,submitted_at)
                VALUES (?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE body_text=VALUES(body_text),word_count=VALUES(word_count),
                submission_status=VALUES(submission_status),self_score=VALUES(self_score),submitted_at=VALUES(submitted_at)
                """,learnerId,promptId,body,words,request.status(),request.selfScore(),submitted);
        return jdbc.queryForObject("SELECT * FROM english_writing_submission WHERE learner_id=? AND prompt_id=?",
                this::view,learnerId,promptId);
    }
    private WritingSubmissionView view(ResultSet row,int ignored) throws SQLException {
        return new WritingSubmissionView(row.getLong("id"),row.getLong("prompt_id"),row.getString("body_text"),
                row.getInt("word_count"),row.getString("submission_status"),row.getBigDecimal("self_score"),
                date(row,"submitted_at"),date(row,"updated_at"));
    }
    private String date(ResultSet row,String column) throws SQLException {
        var timestamp=row.getTimestamp(column);
        return timestamp==null?null:timezone.atSite(timestamp.toLocalDateTime()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
