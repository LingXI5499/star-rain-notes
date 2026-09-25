package com.starrainnotes.english.shared.exercise.service;

import com.starrainnotes.english.shared.exercise.api.EnglishExerciseRemovalPort;
import com.starrainnotes.english.shared.exercise.mapper.EnglishExerciseMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class EnglishExerciseRemoval implements EnglishExerciseRemovalPort {
    private final EnglishExerciseMapper mapper;

    public EnglishExerciseRemoval(EnglishExerciseMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void deleteAll(Collection<Long> ids) {
        if (ids == null) {
            return;
        }
        for (Long id : ids) {
            if (id != null) {
                mapper.deleteById(id);
            }
        }
    }
}
