package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.english.reading.dto.ReadingDto;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class EnglishRetirementContractTest {
    @Test
    void scannedControllerRoutesHaveNoLegacyWritingHandlers() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<String> routes = new HashSet<>();
        for (var bean : scanner.findCandidateComponents("com.starrainnotes.english")) {
            Class<?> type = Class.forName(bean.getBeanClassName());
            RequestMapping mapping = type.getAnnotation(RequestMapping.class);
            if (mapping != null) routes.addAll(List.of(mapping.value()));
        }
        for (String scope : List.of("public", "admin")) {
            for (String legacy : List.of("writing-resources", "writing-prompts")) {
                String removed = "/api/" + scope + "/english/content/" + legacy;
                assertTrue(routes.stream().noneMatch(path -> path.startsWith(removed)), removed);
            }
        }
        assertTrue(routes.contains("/api/public/english/content/reading"));
        assertTrue(routes.contains("/api/admin/english/content/reading"));
        assertTrue(routes.contains("/api/account/english/writing/articles"));
        assertTrue(routes.contains("/api/public/english/content/writing-articles"));
    }

    @Test
    void readingDtoDropsLegacyFieldsWhileAcceptingHistoricalSnapshots() throws Exception {
        ObjectMapper json = new ObjectMapper().findAndRegisterModules();
        var legacy = json.readTree("""
                {"title":"Historical reading","bodyMarkdown":"English body.",
                "cefrLevel":"A1","difficultyLevel":1,"levelAssessed":true}
                """);
        var article = json.treeToValue(legacy, ReadingDto.Article.class);
        var request = json.treeToValue(legacy, ReadingDto.Request.class);
        assertEquals("Historical reading", article.getTitle());
        assertEquals("English body.", request.getBodyMarkdown());
        for (Object dto : List.of(article, request)) {
            var result = json.valueToTree(dto);
            for (String name : List.of("cefrLevel", "difficultyLevel", "levelAssessed")) {
                assertFalse(result.has(name));
            }
        }
    }
}
