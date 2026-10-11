/*
 * VocabularyAdminController：英语学习模块的HTTP 接入文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、VocabularyDto、VocabularyService。
 * 阅读入口：createTheme（创建主题）；updateTheme（更新主题）；deleteTheme（删除主题）；createWord（创建单词）。
 */

package com.starrainnotes.english.vocabulary.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto;
import com.starrainnotes.english.vocabulary.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 词汇后台控制器，属于英语学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/english/vocabulary")
@PreAuthorize("hasAuthority('english:content-edit')")
public class VocabularyAdminController {
    /**
     * 业务服务。
     * 协作类型：<code>VocabularyService</code>。
     */
    private final VocabularyService service;

    /**
     * 创建主题。
     * HTTP 映射：<code>@PostMapping("/themes")</code>。
     * 主要协作调用：<code>service.createTheme</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>VocabularyDto.ThemeRequest</code>
     * @return 类型为 <code>ApiResponse&lt;VocabularyDto.Theme&gt;</code> 的处理结果
     */
    @PostMapping("/themes")
    public ApiResponse<VocabularyDto.Theme> createTheme(@RequestBody VocabularyDto.ThemeRequest request) {
        return ApiResponse.ok(service.createTheme(request));
    }

    /**
     * 更新主题。
     * HTTP 映射：<code>@PutMapping("/themes/{themeId}")</code>。
     * 主要协作调用：<code>service.updateTheme</code>。
     *
     * @param themeId 主题标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>VocabularyDto.ThemeRequest</code>
     * @return 类型为 <code>ApiResponse&lt;VocabularyDto.Theme&gt;</code> 的处理结果
     */
    @PutMapping("/themes/{themeId}")
    public ApiResponse<VocabularyDto.Theme> updateTheme(@PathVariable String themeId,
                                                             @RequestBody VocabularyDto.ThemeRequest request) {
        return ApiResponse.ok(service.updateTheme(themeId, request));
    }

    /**
     * 删除主题。
     * HTTP 映射：<code>@DeleteMapping("/themes/{themeId}")</code>。
     * 主要协作调用：<code>service.deleteTheme</code>。
     *
     * @param themeId 主题标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @DeleteMapping("/themes/{themeId}")
    public ApiResponse<Void> deleteTheme(@PathVariable String themeId) {
        service.deleteTheme(themeId);
        return ApiResponse.ok(null);
    }

    /**
     * 创建单词。
     * HTTP 映射：<code>@PostMapping("/words")</code>。
     * 主要协作调用：<code>service.createWord</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>VocabularyDto.WordRequest</code>
     * @return 类型为 <code>ApiResponse&lt;VocabularyDto.Word&gt;</code> 的处理结果
     */
    @PostMapping("/words")
    public ApiResponse<VocabularyDto.Word> createWord(@RequestBody VocabularyDto.WordRequest request) {
        return ApiResponse.ok(service.createWord(request));
    }

    /**
     * 更新单词。
     * HTTP 映射：<code>@PutMapping("/words/{wordId}")</code>。
     * 主要协作调用：<code>service.updateWord</code>。
     *
     * @param wordId 单词标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>VocabularyDto.WordRequest</code>
     * @return 类型为 <code>ApiResponse&lt;VocabularyDto.Word&gt;</code> 的处理结果
     */
    @PutMapping("/words/{wordId}")
    public ApiResponse<VocabularyDto.Word> updateWord(@PathVariable String wordId,
                                                           @RequestBody VocabularyDto.WordRequest request) {
        return ApiResponse.ok(service.updateWord(wordId, request));
    }

    /**
     * 删除单词。
     * HTTP 映射：<code>@DeleteMapping("/words/{wordId}")</code>。
     * 主要协作调用：<code>service.deleteWord</code>。
     *
     * @param wordId 单词标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @DeleteMapping("/words/{wordId}")
    public ApiResponse<Void> deleteWord(@PathVariable String wordId) {
        service.deleteWord(wordId);
        return ApiResponse.ok(null);
    }
}
