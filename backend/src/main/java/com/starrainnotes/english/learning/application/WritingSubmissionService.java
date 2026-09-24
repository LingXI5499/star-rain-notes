package com.starrainnotes.english.learning.application;

import com.starrainnotes.english.learning.dto.WritingSubmissionRequest;
import com.starrainnotes.english.learning.dto.WritingSubmissionView;
import com.starrainnotes.english.learning.infrastructure.WritingSubmissionRepository;
import com.starrainnotes.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Set;

@Service
public class WritingSubmissionService {
    private final LearnerProfileService profiles;
    private final WritingSubmissionRepository repository;
    public WritingSubmissionService(LearnerProfileService profiles,WritingSubmissionRepository repository) {
        this.profiles=profiles; this.repository=repository;
    }
    public Map<String,Object> get(long accountId,long promptId) {
        Long profile=profiles.profileId(accountId);
        if(profile==null) throw new ApiException(HttpStatus.NOT_FOUND,"WRITING_SUBMISSION_NOT_FOUND","Not found","The requested data does not exist.");
        return repository.get(profile,promptId);
    }
    public WritingSubmissionView getView(long accountId, long promptId) {
        Long profile = profiles.profileId(accountId);
        if (profile == null) throw new ApiException(HttpStatus.NOT_FOUND,
                "WRITING_SUBMISSION_NOT_FOUND", "Not found", "The requested data does not exist.");
        return repository.getView(profile, promptId);
    }
    @Transactional public WritingSubmissionView save(long accountId,long promptId,WritingSubmissionRequest request) {
        if(!Set.of("DRAFT","SUBMITTED").contains(request.status()))
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"ENGLISH_LEARNING_RECORD_INVALID","Invalid learning record","Unknown submission status.");
        repository.requirePublishedPrompt(promptId);
        long profile=profiles.ensureAccountProfile(accountId);
        return repository.save(profile,promptId,request);
    }
}
