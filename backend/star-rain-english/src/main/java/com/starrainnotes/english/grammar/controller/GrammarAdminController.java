package com.starrainnotes.english.grammar.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.grammar.dto.GrammarDto;
import com.starrainnotes.english.grammar.service.GrammarService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/english/grammar")
@PreAuthorize("hasAuthority('english:content-read-admin')")
public class GrammarAdminController {
    private final GrammarService service;

    @GetMapping
    public ApiResponse<GrammarDto.Curriculum> curriculum() {
        return ApiResponse.ok(service.curriculum(true));
    }

    @GetMapping("/lessons/{lessonId}")
    public ApiResponse<GrammarDto.Lesson> lesson(@PathVariable String lessonId) {
        return ApiResponse.ok(service.lesson(lessonId, true));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<GrammarDto.Course> updateCourse(@RequestBody GrammarDto.CourseRequest request) {
        return ApiResponse.ok(service.updateCourse(request));
    }

    @PostMapping("/{action:publish|withdraw}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Void> courseStatus(@PathVariable String action) {
        service.setCoursePublished("publish".equals(action));
        return ApiResponse.ok(null);
    }

    @PostMapping("/sections")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<GrammarDto.Section> createSection(@RequestBody GrammarDto.SectionRequest request) {
        return ApiResponse.ok(service.createSection(request));
    }

    @PutMapping("/sections/{sectionId}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<GrammarDto.Section> updateSection(@PathVariable String sectionId,
                                                              @RequestBody GrammarDto.SectionRequest request) {
        return ApiResponse.ok(service.updateSection(sectionId, request));
    }

    @DeleteMapping("/sections/{sectionId}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Void> deleteSection(@PathVariable String sectionId) {
        service.deleteSection(sectionId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/lessons")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<GrammarDto.Lesson> createLesson(@RequestBody GrammarDto.LessonRequest request) {
        return ApiResponse.ok(service.createLesson(request));
    }

    @PutMapping("/lessons/{lessonId}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<GrammarDto.Lesson> updateLesson(@PathVariable String lessonId,
                                                            @RequestBody GrammarDto.LessonRequest request) {
        return ApiResponse.ok(service.updateLesson(lessonId, request));
    }

    @PostMapping("/lessons/{lessonId}/{action:publish|withdraw}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<GrammarDto.Lesson> lessonStatus(@PathVariable String lessonId,
                                                            @PathVariable String action) {
        return ApiResponse.ok(service.setLessonPublished(lessonId, "publish".equals(action)));
    }

    @DeleteMapping("/lessons/{lessonId}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Void> deleteLesson(@PathVariable String lessonId) {
        service.deleteLesson(lessonId);
        return ApiResponse.ok(null);
    }
}
