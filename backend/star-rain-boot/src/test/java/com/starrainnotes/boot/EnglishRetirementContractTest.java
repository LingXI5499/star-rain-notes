/*
 * EnglishRetirementContractTest：应用启动模块的测试文件。
 * 业务范围：多模块装配、入口路由、安全配置和启动初始化。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：ReadingDto。
 * 阅读入口：scannedControllerRoutesHaveNoLegacyWritingHandlers（scanned控制器路由集合Have不含Legacy写作Handlers处理入口）；readingDtoDropsLegacyFieldsWhileAcceptingHistoricalSnapshots（阅读传输对象DropsLegacy字段集合WhileAcceptingHistoricalSnapshots处理入口）。
 */

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

/**
 * 英语RetirementContract测试，属于应用启动模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class EnglishRetirementContractTest {
    /**
     * scanned控制器路由集合Have不含Legacy写作Handlers处理入口。
     * 关键处理步骤调用 <code>scanner.addIncludeFilter</code>、<code>scanner.findCandidateComponents</code>、<code>Class.forName</code>、<code>bean.getBeanClassName</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
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

    /**
     * 阅读传输对象DropsLegacy字段集合WhileAcceptingHistoricalSnapshots处理入口。
     * 关键处理步骤调用 <code>json.readTree</code>、<code>json.treeToValue</code>、<code>article.getTitle</code>、<code>request.getBodyMarkdown</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
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
