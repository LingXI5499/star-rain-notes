package com.starrainnotes.portfolio.api;

import com.starrainnotes.portfolio.mapper.PortfolioProjectMapper;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class PortfolioLookup implements PortfolioLookupPort {
    private final PortfolioProjectMapper mapper;

    public PortfolioLookup(PortfolioProjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean containsAll(Collection<Long> ids) {
        return ids.isEmpty() || mapper.selectBatchIds(ids).size() == ids.size();
    }

    @Override
    public List<PortfolioLookupPort.Ref> findAll(Collection<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return mapper.selectBatchIds(ids).stream()
                .map(row -> new PortfolioLookupPort.Ref(row.getId(), row.getTitle(), row.getSlug(), row.getPublishStatus()))
                .toList();
    }
}
