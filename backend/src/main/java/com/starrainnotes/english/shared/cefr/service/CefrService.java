package com.starrainnotes.english.shared.cefr.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.english.shared.cefr.dto.CefrLevelView;
import com.starrainnotes.english.shared.cefr.entity.EnglishCefrStandard;
import com.starrainnotes.english.shared.cefr.mapper.EnglishCefrStandardMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Read-only CEFR standard access (方案 §5.2). Levels are seeded by V9 and
 * referenced by content; admin cannot edit them as free-form, so this service
 * exposes only reads for the pickers and release reminders.
 */
@Service
public class CefrService {

    private static final List<String> ORDER = List.of("A1", "A2", "B1", "B2", "C1", "C2");

    private final EnglishCefrStandardMapper mapper;

    public CefrService(EnglishCefrStandardMapper mapper) {
        this.mapper = mapper;
    }

    public List<CefrLevelView> list() {
        return mapper.selectList(new LambdaQueryWrapper<EnglishCefrStandard>()).stream()
                .sorted(Comparator.comparingInt(level -> ORDER.indexOf(level.getLevel())))
                .map(this::toView)
                .toList();
    }

    private CefrLevelView toView(EnglishCefrStandard c) {
        return new CefrLevelView(c.getLevel(), c.getVocabMin(), c.getVocabMax(),
                c.getReadingSentenceMin(), c.getReadingSentenceMax(),
                c.getListeningWpmMin(), c.getListeningWpmMax(),
                c.getWritingLengthMin(), c.getWritingLengthMax(), c.getDescription());
    }
}
