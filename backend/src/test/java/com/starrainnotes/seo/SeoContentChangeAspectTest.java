package com.starrainnotes.seo;

import com.starrainnotes.blog.api.BlogSeoPort;
import com.starrainnotes.portfolio.api.PortfolioSeoPort;
import com.starrainnotes.tutorial.api.TutorialSeoPort;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeoContentChangeAspectTest {
    @Test
    void usesDeclaredIdParameterForVisibilityAndNotification() throws Throwable {
        TutorialSeoPort tutorials = mock(TutorialSeoPort.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        SeoContentChangeAspect aspect = new SeoContentChangeAspect(tutorials, mock(BlogSeoPort.class),
                mock(PortfolioSeoPort.class), events,
                new SeoProperties("https://yulanlin.cn", null, null, false, null, false, null, null));
        ProceedingJoinPoint invocation = mock(ProceedingJoinPoint.class);
        SeoContentChange change = mock(SeoContentChange.class);
        when(invocation.getArgs()).thenReturn(new Object[] {99L, 42L});
        when(invocation.proceed()).thenReturn("updated");
        when(change.kind()).thenReturn("tutorial");
        when(change.pathPrefix()).thenReturn("/tutorials/");
        when(change.idParameter()).thenReturn(1);
        when(tutorials.visibility(42L)).thenReturn(new TutorialSeoPort.State("lesson", true));

        assertThat(aspect.publishAfterChange(invocation, change)).isEqualTo("updated");

        verify(tutorials, org.mockito.Mockito.times(2)).visibility(42L);
        var event = org.mockito.ArgumentCaptor.forClass(SeoContentChangedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().absoluteUrl()).isEqualTo("https://yulanlin.cn/tutorials/lesson");
    }

    @Test
    void publishedSlugChangeNotifiesBothOldAndNewUrls() throws Throwable {
        TutorialSeoPort tutorials = mock(TutorialSeoPort.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        SeoContentChangeAspect aspect = new SeoContentChangeAspect(tutorials, mock(BlogSeoPort.class),
                mock(PortfolioSeoPort.class), events,
                new SeoProperties("https://yulanlin.cn", null, null, false, null, false, null, null));
        ProceedingJoinPoint invocation = mock(ProceedingJoinPoint.class);
        SeoContentChange change = mock(SeoContentChange.class);
        when(invocation.getArgs()).thenReturn(new Object[] {42L});
        when(change.kind()).thenReturn("tutorial");
        when(change.pathPrefix()).thenReturn("/tutorials/");
        when(tutorials.visibility(42L)).thenReturn(
                new TutorialSeoPort.State("old-slug", true),
                new TutorialSeoPort.State("new-slug", true));

        aspect.publishAfterChange(invocation, change);

        var eventsSent = org.mockito.ArgumentCaptor.forClass(SeoContentChangedEvent.class);
        verify(events, org.mockito.Mockito.times(2)).publishEvent(eventsSent.capture());
        assertThat(eventsSent.getAllValues()).extracting(SeoContentChangedEvent::absoluteUrl)
                .containsExactly("https://yulanlin.cn/tutorials/old-slug",
                        "https://yulanlin.cn/tutorials/new-slug");
    }
}
