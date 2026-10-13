package com.starrainnotes.blog.service.impl;

import com.starrainnotes.blog.entity.BlogTagEntity;
import com.starrainnotes.blog.exception.BlogTagInUseException;
import com.starrainnotes.blog.exception.BlogTagNotFoundException;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.blog.service.BlogViewAssembler;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** An unused taxonomy record may be removed without changing any article or binding. */
class BlogTagDeletionTest {
    @Test void locksThenDeletesUnusedTag() {
        BlogTagMapper mapper = mock(BlogTagMapper.class);
        when(mapper.tagByIdForUpdate(1L)).thenReturn(new BlogTagEntity());
        when(mapper.deleteUnusedTag(1L)).thenReturn(1);
        new BlogTagServiceImpl(mapper, mock(BlogViewAssembler.class)).delete(1L);
        var order = inOrder(mapper); order.verify(mapper).tagByIdForUpdate(1L);
        order.verify(mapper).countReferences(1L); order.verify(mapper).deleteUnusedTag(1L);
    }
    @Test void refusesEveryExistingBindingAndDoesNotCascade() {
        BlogTagMapper mapper = mock(BlogTagMapper.class);
        when(mapper.tagByIdForUpdate(1L)).thenReturn(new BlogTagEntity());
        when(mapper.countReferences(1L)).thenReturn(1L);
        assertThatThrownBy(() -> new BlogTagServiceImpl(mapper, mock(BlogViewAssembler.class)).delete(1L)).isInstanceOf(BlogTagInUseException.class);
        verify(mapper, never()).deleteUnusedTag(any());
    }
    @Test void missingTagReturnsTheExistingNotFoundContract() {
        assertThatThrownBy(() -> new BlogTagServiceImpl(mock(BlogTagMapper.class), mock(BlogViewAssembler.class)).delete(99L)).isInstanceOf(BlogTagNotFoundException.class);
    }
}
