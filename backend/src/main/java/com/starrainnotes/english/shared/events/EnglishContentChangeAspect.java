package com.starrainnotes.english.shared.events;

import com.starrainnotes.english.shared.events.infrastructure.EnglishContentStateRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class EnglishContentChangeAspect {
    private final EnglishContentStateRepository repository;
    private final ApplicationEventPublisher events;

    public EnglishContentChangeAspect(EnglishContentStateRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Around("@annotation(change)")
    public Object publishAfterChange(ProceedingJoinPoint invocation, EnglishContentChange change) throws Throwable {
        Long id = firstLong(invocation.getArgs());
        if (id == null) throw new IllegalStateException("Content change requires a content ID");
        EnglishContentState before = repository.state(change.kind(), id);
        Object result = invocation.proceed();
        EnglishContentState after = repository.state(change.kind(), id);
        EnglishContentState visible = after != null && after.published() ? after
                : before != null && before.published() ? before : null;
        if (visible != null) {
            events.publishEvent(new EnglishContentChangedEvent(change.kind(), id,
                    visible.slug(), change.changeType()));
        }
        return result;
    }

    /** TODO(W2-16b): declare the id parameter on {@link EnglishContentChange} instead of scanning arguments. */
    private Long firstLong(Object[] args) {
        for (Object arg : args) if (arg instanceof Long value) return value;
        return null;
    }

}
