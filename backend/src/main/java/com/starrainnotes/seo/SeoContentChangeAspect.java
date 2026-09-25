package com.starrainnotes.seo;

import com.starrainnotes.blog.api.BlogSeoPort;
import com.starrainnotes.portfolio.api.PortfolioSeoPort;
import com.starrainnotes.tutorial.api.TutorialSeoPort;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Aspect
@Component
public class SeoContentChangeAspect {
    private final TutorialSeoPort tutorials;
    private final BlogSeoPort blogs;
    private final PortfolioSeoPort projects;
    private final ApplicationEventPublisher events;
    private final SeoProperties properties;

    public SeoContentChangeAspect(TutorialSeoPort tutorials, BlogSeoPort blogs, PortfolioSeoPort projects,
                                  ApplicationEventPublisher events, SeoProperties properties) {
        this.tutorials = tutorials;
        this.blogs = blogs;
        this.projects = projects;
        this.events = events;
        this.properties = properties;
    }

    @Around("@annotation(change)")
    public Object publishAfterChange(ProceedingJoinPoint invocation, SeoContentChange change) throws Throwable {
        Long id = contentId(invocation.getArgs(), change.idParameter());
        ContentState before = state(change.kind(), id);
        Object result = invocation.proceed();
        ContentState current = state(change.kind(), id);
        if (before != null && before.published()
                && (current == null || !current.published() || !Objects.equals(before.slug(), current.slug()))) {
            publish(change, before.slug());
        }
        if (current != null && current.published()) publish(change, current.slug());
        return result;
    }

    private void publish(SeoContentChange change, String slug) {
        events.publishEvent(new SeoContentChangedEvent(properties.siteOrigin() + change.pathPrefix() + slug));
    }

    private ContentState state(String kind, Long id) {
        return switch (kind) {
            case "tutorial" -> map(tutorials.visibility(id));
            case "blog" -> map(blogs.visibility(id));
            case "portfolio" -> map(projects.visibility(id));
            default -> throw new IllegalArgumentException("Unsafe SEO kind");
        };
    }

    private ContentState map(Object state) {
        if (state instanceof TutorialSeoPort.State row) return new ContentState(row.slug(), row.published());
        if (state instanceof BlogSeoPort.State row) return new ContentState(row.slug(), row.published());
        if (state instanceof PortfolioSeoPort.State row) return new ContentState(row.slug(), row.published());
        return null;
    }

    private Long contentId(Object[] args, int index) {
        if (index < 0 || index >= args.length || !(args[index] instanceof Long id)) {
            throw new IllegalStateException("@SeoContentChange idParameter must refer to a Long content id");
        }
        return id;
    }
    private record ContentState(String slug, boolean published) {}
}
