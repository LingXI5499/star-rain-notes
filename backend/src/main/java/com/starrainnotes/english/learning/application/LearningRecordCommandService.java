package com.starrainnotes.english.learning.application;

import com.starrainnotes.english.learning.infrastructure.LearningRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningRecordCommandService {
    private final LearnerProfileService profiles;
    private final LearningRecordRepository records;
    public LearningRecordCommandService(LearnerProfileService profiles,LearningRecordRepository records) { this.profiles=profiles; this.records=records; }
    @Transactional public void saveSimple(long accountId,String type,long contentId,String status,Integer seconds) { records.saveSimple(profiles.ensureAccountProfile(accountId),type,contentId,status,seconds); }
}
