package com.starrainnotes.english.learning.application;

import com.starrainnotes.english.learning.infrastructure.LearnerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearnerProfileService {
    private final LearnerProfileRepository repository;
    public LearnerProfileService(LearnerProfileRepository repository) { this.repository=repository; }
    @Transactional public long ensureAccountProfile(long accountId) {
        Long id=repository.findByAccountId(accountId);
        return id==null?repository.createAccountProfile(accountId):id;
    }
    public Long profileId(long accountId) { return repository.findByAccountId(accountId); }
    @Transactional public long claimLegacy(long accountId,String hash) {
        Long existing=repository.findByAccountId(accountId);
        if(existing!=null) return existing;
        Long legacy=repository.findByLegacyKeyHash(hash);
        if(legacy==null) return -1L;
        repository.claim(accountId,legacy);
        return legacy;
    }
}
