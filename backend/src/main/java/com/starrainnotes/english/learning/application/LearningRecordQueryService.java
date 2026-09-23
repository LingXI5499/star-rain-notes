package com.starrainnotes.english.learning.application;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.learning.infrastructure.LearningRecordRepository;
import com.starrainnotes.english.learning.dto.LearningRecordView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class LearningRecordQueryService {
    private final LearnerProfileService profiles;
    private final LearningRecordRepository records;
    private static final Set<String> TYPES = Set.of("GRAMMAR", "READING", "LISTENING", "WRITING");
    public LearningRecordQueryService(LearnerProfileService profiles,LearningRecordRepository records) { this.profiles=profiles; this.records=records; }
    public List<Map<String,Object>> list(long accountId,int page,int size) { Long profile=profiles.profileId(accountId); return profile==null?List.of():records.list(profile,page,size); }
    public Map<String,Object> get(long accountId,String type,long contentId) { Long profile=profiles.profileId(accountId); if(profile==null) throw notFound(); return records.get(profile,type,contentId); }
    public Map<String,LearningRecordView> batch(long accountId,List<String> refs) {
        Long profile=profiles.profileId(accountId);
        if(profile==null||refs==null||refs.isEmpty()) return Map.of();
        if(refs.size()>100) invalid("At most 100 learning records can be requested at once.");
        LinkedHashSet<LearningRecordRepository.ContentRef> normalized=new LinkedHashSet<>();
        for(String ref:refs) {
            String[] parts=ref==null?new String[0]:ref.split(":",2);
            if(parts.length!=2) invalid("Invalid learning record reference.");
            String type=parts[0].toUpperCase(Locale.ROOT);
            if(!TYPES.contains(type)) invalid("Unknown content type.");
            long contentId;
            try { contentId=Long.parseLong(parts[1]); }
            catch(NumberFormatException ex) { invalid("Invalid learning record reference."); return Map.of(); }
            if(contentId<=0) invalid("Invalid learning record reference.");
            normalized.add(new LearningRecordRepository.ContentRef(type,contentId));
        }
        return records.batch(profile,List.copyOf(normalized));
    }
    private void invalid(String detail) { throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"ENGLISH_LEARNING_RECORD_INVALID","Invalid learning record",detail); }
    private ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND,"LEARNING_RECORD_NOT_FOUND","Not found","The requested data does not exist."); }
}
