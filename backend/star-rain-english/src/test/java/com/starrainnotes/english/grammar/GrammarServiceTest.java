package com.starrainnotes.english.grammar;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.grammar.dto.GrammarDto.*;
import com.starrainnotes.english.grammar.mapper.GrammarMapper;
import com.starrainnotes.english.grammar.service.impl.GrammarServiceImpl;
import com.starrainnotes.english.api.event.EnglishSearchContentChangedEvent;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GrammarServiceTest {
    private final GrammarMapper mapper = mock(GrammarMapper.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final GrammarServiceImpl service = new GrammarServiceImpl(mapper, events);

    @Test void deletingSectionDeletesItsLessonsBeforeTheParentAndRefreshesSearch() {
        when(mapper.lockCourse()).thenReturn(new Course());
        Section section = new Section(); section.setId(7L);
        when(mapper.section(7L)).thenReturn(section);
        when(mapper.lessons(7L, false)).thenReturn(List.of());
        when(mapper.deleteSection(7L)).thenReturn(1);
        service.deleteSection("7");
        var order = inOrder(mapper);
        order.verify(mapper).lockCourse(); order.verify(mapper).section(7L);
        order.verify(mapper).lessons(7L, false); order.verify(mapper).deleteLessonsInSection(7L); order.verify(mapper).deleteSection(7L);
        verify(events).publishEvent(any(EnglishSearchContentChangedEvent.class));
    }

    @Test void missingSectionDoesNotDeleteAnyLessons() {
        when(mapper.lockCourse()).thenReturn(new Course());
        assertThatThrownBy(() -> service.deleteSection("77")).isInstanceOf(ApiException.class);
        verify(mapper, never()).deleteLessonsInSection(anyLong());
    }

    @Test void orderingRequiresExactlyTheCurrentSiblingSet() {
        when(mapper.lockCourse()).thenReturn(new Course());
        Section first = new Section(); first.setId(1L); Section last = new Section(); last.setId(2L);
        when(mapper.sections()).thenReturn(List.of(first, last));
        assertThatThrownBy(() -> service.reorderSections(List.of("1", "1"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.reorderSections(List.of("1", "9"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.reorderSections(List.of("1"))).isInstanceOf(ApiException.class);
        verify(mapper, never()).orderSection(anyLong(), anyInt());
        service.reorderSections(List.of("2", "1"));
        verify(mapper).orderSection(2L, 10); verify(mapper).orderSection(1L, 20);
    }

    @Test void lessonOrderingCannotIncludeOtherSectionsLessons() {
        when(mapper.lockCourse()).thenReturn(new Course());
        Section section = new Section(); section.setId(7L); Lesson lesson = new Lesson(); lesson.setId(8L);
        when(mapper.section(7L)).thenReturn(section); when(mapper.lessons(7L, false)).thenReturn(List.of(lesson));
        assertThatThrownBy(() -> service.reorderLessons("7", List.of("99"))).isInstanceOf(ApiException.class);
        verify(mapper, never()).orderLesson(anyLong(), anyLong(), anyInt());
    }
}
