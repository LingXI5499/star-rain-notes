package com.starrainnotes.english.vocabulary.application;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.api.MediaPort;
import com.starrainnotes.english.vocabulary.dto.VocabularyAudioRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyAudioView;
import com.starrainnotes.english.vocabulary.infrastructure.VocabularyAudioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VocabularyAudioService {
    private final VocabularyAudioRepository repository;
    private final MediaPort media;

    public VocabularyAudioService(VocabularyAudioRepository repository, MediaPort media) {
        this.repository = repository;
        this.media = media;
    }

    @Transactional
    public VocabularyAudioView addAudio(long wordId, VocabularyAudioRequest request) {
        String type = media.assetType(request.mediaAssetId());
        if (type == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND", "Media not found",
                    "The selected media asset does not exist.");
        }
        if (!"AUDIO".equals(type)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VOCABULARY_AUDIO_REQUIRED", "Audio required",
                    "The selected media asset must be an audio file.");
        }
        return repository.add(wordId, request);
    }

    @Transactional
    public void deleteAudio(long wordId, long audioId) {
        repository.delete(wordId, audioId);
    }

    @Transactional
    public VocabularyAudioView setPrimaryAudio(long wordId, long audioId) {
        return repository.setPrimary(wordId, audioId);
    }
}
