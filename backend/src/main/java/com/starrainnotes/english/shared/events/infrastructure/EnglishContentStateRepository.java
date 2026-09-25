package com.starrainnotes.english.shared.events.infrastructure;

import com.starrainnotes.english.shared.events.EnglishContentKind;
import com.starrainnotes.english.shared.events.EnglishContentState;
import com.starrainnotes.english.shared.events.EnglishContentStateSource;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Published-state lookup shared by content events and review capability. */
@Repository
public class EnglishContentStateRepository {
    private final Map<EnglishContentKind, EnglishContentStateSource> sources;

    public EnglishContentStateRepository(List<EnglishContentStateSource> sources) {
        Map<EnglishContentKind, EnglishContentStateSource> selected = new EnumMap<>(EnglishContentKind.class);
        for (EnglishContentStateSource source : sources) {
            if (selected.putIfAbsent(source.kind(), source) != null) {
                throw new IllegalStateException("Duplicate English content state source: " + source.kind());
            }
        }
        if (selected.size() != EnglishContentKind.values().length) {
            throw new IllegalStateException("Missing English content state source");
        }
        this.sources = Map.copyOf(selected);
    }

    public EnglishContentState state(EnglishContentKind kind, long id) {
        return sources.get(kind).find(id);
    }
}
