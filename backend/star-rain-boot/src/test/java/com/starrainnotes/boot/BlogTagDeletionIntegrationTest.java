package com.starrainnotes.boot;

import com.starrainnotes.blog.entity.BlogTagEntity;
import com.starrainnotes.blog.exception.BlogTagInUseException;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.blog.service.BlogViewAssembler;
import com.starrainnotes.blog.service.impl.BlogTagServiceImpl;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Real SQL refuses referenced tags and deletes only the requested unused row. */
class BlogTagDeletionIntegrationTest extends MapperXmlIntegrationSupport {
    @Test void referencePreventsDeletionUntilExplicitlyUnbound() {
        try (SqlSession session = openSession()) {
            BlogTagMapper mapper = session.getMapper(BlogTagMapper.class);
            BlogTagEntity tag = new BlogTagEntity();
            String name = "v21-tag-" + System.nanoTime();
            tag.setName(name); tag.setSlug(name); tag.setStatus("ENABLED");
            mapper.insertTag(tag);
            mapper.insertPostTag(987654321L, tag.getId());
            BlogTagServiceImpl service = new BlogTagServiceImpl(mapper, mock(BlogViewAssembler.class));
            assertThat(mapper.deleteUnusedTag(tag.getId())).isZero();
            assertThatThrownBy(() -> service.delete(tag.getId())).isInstanceOf(BlogTagInUseException.class);
            mapper.deletePostTag(987654321L, tag.getId());
            service.delete(tag.getId());
            assertThat(mapper.tagById(tag.getId())).isNull();
            session.rollback();
        }
    }
}
