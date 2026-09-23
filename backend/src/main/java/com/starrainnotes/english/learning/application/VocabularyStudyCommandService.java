package com.starrainnotes.english.learning.application;

import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VocabularyStudyCommandService {
    private final JdbcTemplate jdbc;
    public VocabularyStudyCommandService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Transactional public void putMemory(long accountId,long wordId,int memoryCount) {
        if(memoryCount<0) throw new ApiException(HttpStatus.NOT_FOUND,"INVALID_MEMORY_COUNT","Not found","The requested data does not exist.");
        int updated=jdbc.update("UPDATE account_vocabulary_memory SET memory_count=?,last_memory_at=UTC_TIMESTAMP(6) WHERE account_id=? AND word_id=?",memoryCount,accountId,wordId);
        if(updated==0) jdbc.update("INSERT INTO account_vocabulary_memory(account_id,word_id,memory_count,last_memory_at) VALUES (?,?,?,UTC_TIMESTAMP(6))",accountId,wordId,memoryCount);
    }
}
