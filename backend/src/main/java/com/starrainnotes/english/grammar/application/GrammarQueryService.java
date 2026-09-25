package com.starrainnotes.english.grammar.application;

import com.starrainnotes.english.grammar.domain.GrammarPublishPolicy;
import com.starrainnotes.english.grammar.dto.GrammarCourseView;
import com.starrainnotes.english.grammar.dto.GrammarCurriculumView;
import com.starrainnotes.english.grammar.dto.GrammarLessonDetailView;
import com.starrainnotes.english.grammar.dto.GrammarLessonSummaryView;
import com.starrainnotes.english.grammar.dto.GrammarSectionView;
import com.starrainnotes.english.grammar.infrastructure.GrammarRepository;
import com.starrainnotes.english.shared.content.ContentDescriptor;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read use cases for the singleton Grammar course and its curriculum. */
@Service
@Transactional(readOnly = true)
public class GrammarQueryService {
    private final GrammarRepository repository;
    private final GrammarPublishPolicy policy;
    public GrammarQueryService(GrammarRepository repository, GrammarPublishPolicy policy) {
        this.repository = repository;
        this.policy = policy;
    }
    public GrammarCourseView course() { return repository.course(); }
    public GrammarCourseView publicCourse() { return repository.publicCourse(); }
    public GrammarCurriculumView curriculum() { return repository.curriculum(); }
    public GrammarCurriculumView publicCurriculum() {
        GrammarCourseView course = repository.publicCourse();
        List<GrammarSectionView> sections = new ArrayList<>();
        for (GrammarSectionView section : repository.curriculum().sections()) {
            List<GrammarLessonSummaryView> published = section.lessons().stream()
                    .filter(lesson -> policy.isPublished(lesson.publishStatus())).toList();
            if (published.isEmpty()) {
                continue;
            }
            sections.add(new GrammarSectionView(section.id(), section.title(), section.sortOrder(),
                    published.size(), published.size(), published));
        }
        return new GrammarCurriculumView(course, sections);
    }
    public GrammarLessonDetailView lesson(long id) { return repository.lesson(id); }
    public GrammarLessonDetailView publicLesson(String slug) { return repository.publicLesson(slug); }
    public long publishedCount() { return repository.publishedCount(); }
    public List<ContentDescriptor> publishedDescriptors() { return repository.publishedDescriptors(); }
}
