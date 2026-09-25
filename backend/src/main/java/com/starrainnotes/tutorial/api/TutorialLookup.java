package com.starrainnotes.tutorial.api;

import com.starrainnotes.tutorial.mapper.TutorialMapper;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class TutorialLookup implements TutorialLookupPort {
    private final TutorialMapper mapper;

    public TutorialLookup(TutorialMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean containsAll(Collection<Long> ids) {
        return ids.isEmpty() || mapper.selectBatchIds(ids).size() == ids.size();
    }

    @Override
    public List<TutorialLookupPort.Ref> findAll(Collection<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return mapper.selectBatchIds(ids).stream()
                .map(row -> new TutorialLookupPort.Ref(row.getId(), row.getTitle(), row.getSlug(), row.getPublishStatus()))
                .toList();
    }
}
