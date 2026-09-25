package com.starrainnotes.english.vocabulary.infrastructure;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.vocabulary.dto.VocabularyAudioRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyAudioView;
import com.starrainnotes.english.vocabulary.mapper.VocabularyWordMapper;
import com.starrainnotes.english.api.MediaPort;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class VocabularyAudioRepository {
    private final JdbcTemplate jdbc;
    private final VocabularyWordMapper words;
    private final MediaPort media;
    public VocabularyAudioRepository(JdbcTemplate jdbc,VocabularyWordMapper words, MediaPort media) {
        this.jdbc=jdbc; this.words=words; this.media=media;
    }

    public VocabularyAudioView add(long wordId,VocabularyAudioRequest request) {
        requireWord(wordId);
        if(request.primary()) jdbc.update("UPDATE vocabulary_word_audio SET is_primary=FALSE WHERE word_id=? AND accent=?",wordId,request.accent());
        jdbc.update("INSERT INTO vocabulary_word_audio(word_id,accent,media_asset_id,provider,source_url,license_note,is_primary) VALUES (?,?,?,?,?,?,?)",
                wordId,request.accent(),request.mediaAssetId(),request.provider()==null?"UPLOADED":request.provider(),clean(request.sourceUrl()),request.licenseNote().trim(),request.primary());
        return audio(jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class));
    }

    public void delete(long wordId,long audioId) {
        requireWord(wordId);
        if(jdbc.update("DELETE FROM vocabulary_word_audio WHERE id=? AND word_id=?",audioId,wordId)==0) throw audioNotFound();
    }

    public VocabularyAudioView setPrimary(long wordId,long audioId) {
        requireWord(wordId); String accent;
        try { accent=jdbc.queryForObject("SELECT accent FROM vocabulary_word_audio WHERE id=? AND word_id=?",String.class,audioId,wordId); }
        catch(EmptyResultDataAccessException ex) { throw audioNotFound(); }
        jdbc.update("UPDATE vocabulary_word_audio SET is_primary=FALSE WHERE word_id=? AND accent=?",wordId,accent);
        jdbc.update("UPDATE vocabulary_word_audio SET is_primary=TRUE WHERE id=?",audioId);
        return audio(audioId);
    }

    private VocabularyAudioView audio(long id) {
        try { return jdbc.queryForObject("""
                SELECT a.id,a.accent,a.media_asset_id,a.provider,a.source_url,a.license_note,a.is_primary
                FROM vocabulary_word_audio a WHERE a.id=?
                """,(rs,n)->new VocabularyAudioView(rs.getLong("id"),rs.getString("accent"),rs.getLong("media_asset_id"),media.publicUrl(rs.getLong("media_asset_id")),rs.getString("provider"),rs.getString("source_url"),rs.getString("license_note"),rs.getBoolean("is_primary")),id); }
        catch(EmptyResultDataAccessException ex) { throw audioNotFound(); }
    }
    private void requireWord(long id) {
        if(words.selectById(id)==null) throw new ApiException(HttpStatus.NOT_FOUND,"VOCABULARY_WORD_NOT_FOUND","Vocabulary word not found","No vocabulary word exists with id "+id+".");
    }
    private String clean(String value) { return value==null||value.isBlank()?null:value.trim(); }
    private ApiException audioNotFound() { return new ApiException(HttpStatus.NOT_FOUND,"VOCABULARY_AUDIO_NOT_FOUND","Audio not found","The pronunciation audio relation does not exist."); }
}
