package com.starrainnotes.english.vocabulary.application;

import com.starrainnotes.english.vocabulary.dto.VocabularyAudioRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyAudioView;
import com.starrainnotes.english.vocabulary.infrastructure.VocabularyAudioRepository;
import org.springframework.stereotype.Service;

@Service
public class VocabularyAudioService {
    private final VocabularyAudioRepository repository;
    public VocabularyAudioService(VocabularyAudioRepository repository) { this.repository = repository; }
    public VocabularyAudioView addAudio(long wordId,VocabularyAudioRequest request) { return repository.add(wordId,request); }
    public void deleteAudio(long wordId,long audioId) { repository.delete(wordId,audioId); }
    public VocabularyAudioView setPrimaryAudio(long wordId,long audioId) { return repository.setPrimary(wordId,audioId); }
}
