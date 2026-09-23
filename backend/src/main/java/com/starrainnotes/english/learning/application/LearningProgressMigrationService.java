package com.starrainnotes.english.learning.application;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.vocabulary.learning.application.VocabularyStudyCommandService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class LearningProgressMigrationService {
    private final LearnerProfileService profiles;
    private final LearningRecordCommandService records;
    private final VocabularyStudyCommandService vocabulary;
    public LearningProgressMigrationService(LearnerProfileService profiles,LearningRecordCommandService records,VocabularyStudyCommandService vocabulary) { this.profiles=profiles; this.records=records; this.vocabulary=vocabulary; }
    @Transactional public long claimLegacy(long accountId,String learnerKeyHash) {
        long profile=profiles.claimLegacy(accountId,learnerKeyHash);
        if(profile<0) throw new ApiException(HttpStatus.NOT_FOUND,"LEGACY_PROFILE_NOT_FOUND","Not found","The requested data does not exist.");
        return profile;
    }
    @Transactional public void importLocal(long accountId,Map<String,Object> payload) {
        profiles.ensureAccountProfile(accountId);
        Object vocab=payload==null?null:payload.get("vocabulary");
        if(vocab instanceof Map<?,?> entries) for(Map.Entry<?,?> entry:entries.entrySet()) {
            long wordId=longValue(entry.getKey()); Object memory=entry.getValue();
            if(wordId>0&&memory instanceof Map<?,?> values) {
                Object count = values.get("memoryCount");
                vocabulary.putMemoryCount(accountId,wordId,Math.max(0,count instanceof Number n?n.intValue():1));
            }
        }
        Object rawRecords=payload==null?null:payload.get("learningRecords");
        if(rawRecords instanceof Map<?,?> entries) for(Map.Entry<?,?> entry:entries.entrySet()) {
            String[] parts=String.valueOf(entry.getKey()).split("/"); if(parts.length!=2) continue;
            long contentId=longValue(parts[1]); if(contentId<=0||!(entry.getValue() instanceof Map<?,?> value)) continue;
            Object status=value.get("completionStatus");
            records.saveSimple(accountId,parts[0],contentId,status==null?"IN_PROGRESS":String.valueOf(status),intObject(value.get("timeSpentSeconds")));
        }
    }
    private long longValue(Object value) { try { return value instanceof Number n?n.longValue():Long.parseLong(String.valueOf(value)); } catch(RuntimeException ex) { return 0; } }
    private Integer intObject(Object value) { return value instanceof Number n?n.intValue():null; }
}
