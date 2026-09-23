package com.starrainnotes.english.learning.infrastructure;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LearnerProfileRepository {
    private final JdbcTemplate jdbc;
    public LearnerProfileRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public Long findByAccountId(long accountId) {
        try { return jdbc.queryForObject("SELECT id FROM english_learner_profile WHERE account_id=? LIMIT 1",Long.class,accountId); }
        catch(EmptyResultDataAccessException ex) { return null; }
    }
    public Long findByLegacyKeyHash(String hash) {
        try { return jdbc.queryForObject("SELECT id FROM english_learner_profile WHERE learner_key_hash=? LIMIT 1",Long.class,hash); }
        catch(EmptyResultDataAccessException ex) { return null; }
    }
    public long createAccountProfile(long accountId) {
        jdbc.update("INSERT INTO english_learner_profile(learner_key_hash,account_id,profile_type,claimed_at) VALUES (NULL,?, 'ACCOUNT', UTC_TIMESTAMP(6))",accountId);
        Long id=findByAccountId(accountId);
        if(id==null) throw new IllegalStateException("Failed to create account learner profile for "+accountId);
        return id;
    }
    public void claim(long accountId,long profileId) {
        jdbc.update("UPDATE english_learner_profile SET account_id=?,profile_type='ACCOUNT',claimed_at=UTC_TIMESTAMP(6) WHERE id=?",accountId,profileId);
    }
}
