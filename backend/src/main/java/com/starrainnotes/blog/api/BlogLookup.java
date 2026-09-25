package com.starrainnotes.blog.api;

import com.starrainnotes.blog.mapper.BlogPostMapper;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class BlogLookup implements BlogLookupPort {
    private final BlogPostMapper mapper;

    public BlogLookup(BlogPostMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean containsAll(Collection<Long> ids) {
        return ids.isEmpty() || mapper.selectBatchIds(ids).size() == ids.size();
    }

    @Override
    public List<BlogLookupPort.Ref> findAll(Collection<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return mapper.selectBatchIds(ids).stream()
                .map(row -> new BlogLookupPort.Ref(row.getId(), row.getTitle(), row.getSlug(), row.getPublishStatus()))
                .toList();
    }
}
