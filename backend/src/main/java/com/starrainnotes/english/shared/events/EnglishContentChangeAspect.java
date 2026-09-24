package com.starrainnotes.english.shared.events;

import com.starrainnotes.english.shared.events.infrastructure.EnglishContentStateRepository;
import com.starrainnotes.english.shared.events.infrastructure.EnglishContentStateRepository.ContentState;
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
        ContentState before = repository.state(change.kind(), id);
        Object result = invocation.proceed();
        ContentState after = repository.state(change.kind(), id);
        ContentState visible = after != null && after.published() ? after
                : before != null && before.published() ? before : null;
        if (visible != null) {
            events.publishEvent(new EnglishContentChangedEvent(change.kind(), id,
                    visible.slug(), change.changeType()));
        }
        return result;
    }

    private Long firstLong(Object[] args) {
        for (Object arg : args) if (arg instanceof Long value) return value;
        return null;
    }

}
