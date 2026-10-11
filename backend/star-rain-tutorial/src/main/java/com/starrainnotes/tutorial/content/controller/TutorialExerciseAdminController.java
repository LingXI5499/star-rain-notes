/*
 * TutorialExerciseAdminController：教程与学习模块的HTTP 接入文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、TutorialOperationService、TutorialPermissions、ChapterQuestionDTO、IdOrderDTO、KnowledgeCardDTO、TutorialExerciseService。
 * 阅读入口：cards（卡片集合处理入口）；createCard（创建卡片）；updateCard（更新卡片）；deleteCard（删除卡片）。
 */

package com.starrainnotes.tutorial.content.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.content.service.TutorialOperationService;
import com.starrainnotes.tutorial.content.constant.TutorialPermissions;
import com.starrainnotes.tutorial.content.dto.ChapterQuestionDTO;
import com.starrainnotes.tutorial.content.dto.IdOrderDTO;
import com.starrainnotes.tutorial.content.dto.KnowledgeCardDTO;
import com.starrainnotes.tutorial.content.service.TutorialExerciseService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教程Exercise后台控制器，属于教程与学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class TutorialExerciseAdminController {
    /**
     * operations。
     * 协作类型：<code>TutorialOperationService</code>。
     */
    private final TutorialOperationService operations;
    /**
     * 业务服务。
     * 协作类型：<code>TutorialExerciseService</code>。
     */
    private final TutorialExerciseService service;

    /**
     * 卡片集合处理入口。
     * HTTP 映射：<code>@GetMapping("/tutorial-chapters/{chapterId}/cards")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>service.cards</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;Map&lt;String, Object&gt;&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/tutorial-chapters/{chapterId}/cards")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<List<Map<String, Object>>> cards(@PathVariable Long chapterId) {
        return ApiResponse.ok(service.cards(chapterId));
    }

    /**
     * 创建卡片。
     * HTTP 映射：<code>@PostMapping("/tutorial-chapters/{chapterId}/cards")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>KnowledgeCardDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PostMapping("/tutorial-chapters/{chapterId}/cards")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> createCard(@PathVariable Long chapterId,
                                                        @Valid @RequestBody KnowledgeCardDTO request) {
        return ApiResponse.ok(operations.request("createCard", new Object[]{chapterId, request}));
    }

    /**
     * 更新卡片。
     * HTTP 映射：<code>@PatchMapping("/tutorial-cards/{cardId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param cardId 卡片标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>KnowledgeCardDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PatchMapping("/tutorial-cards/{cardId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> updateCard(@PathVariable Long cardId,
                                                        @Valid @RequestBody KnowledgeCardDTO request) {
        return ApiResponse.ok(operations.request("updateCard", new Object[]{cardId, request}));
    }

    /**
     * 删除卡片。
     * HTTP 映射：<code>@DeleteMapping("/tutorial-cards/{cardId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param cardId 卡片标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @DeleteMapping("/tutorial-cards/{cardId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> deleteCard(@PathVariable Long cardId) {
        return ApiResponse.ok(operations.request("deleteCard", new Object[]{cardId}));
    }

    /**
     * 调整顺序卡片集合。
     * HTTP 映射：<code>@PutMapping("/tutorial-chapters/{chapterId}/cards/order")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>IdOrderDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PutMapping("/tutorial-chapters/{chapterId}/cards/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> reorderCards(@PathVariable Long chapterId, @Valid @RequestBody IdOrderDTO request) {
        return ApiResponse.ok(operations.request("reorderCards", new Object[]{chapterId, request.getIds()}));
    }

    /**
     * 问题集合处理入口。
     * HTTP 映射：<code>@GetMapping("/tutorial-chapters/{chapterId}/questions")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>service.questions</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;Map&lt;String, Object&gt;&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/tutorial-chapters/{chapterId}/questions")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<List<Map<String, Object>>> questions(@PathVariable Long chapterId) {
        return ApiResponse.ok(service.questions(chapterId));
    }

    /**
     * 创建问题。
     * HTTP 映射：<code>@PostMapping("/tutorial-chapters/{chapterId}/questions")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ChapterQuestionDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PostMapping("/tutorial-chapters/{chapterId}/questions")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> createQuestion(@PathVariable Long chapterId,
                                                            @Valid @RequestBody ChapterQuestionDTO request) {
        return ApiResponse.ok(operations.request("createQuestion", new Object[]{chapterId, request}));
    }

    /**
     * 更新问题。
     * HTTP 映射：<code>@PatchMapping("/tutorial-questions/{questionId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param questionId 问题标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ChapterQuestionDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PatchMapping("/tutorial-questions/{questionId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> updateQuestion(@PathVariable Long questionId,
                                                            @Valid @RequestBody ChapterQuestionDTO request) {
        return ApiResponse.ok(operations.request("updateQuestion", new Object[]{questionId, request}));
    }

    /**
     * 删除问题。
     * HTTP 映射：<code>@DeleteMapping("/tutorial-questions/{questionId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param questionId 问题标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @DeleteMapping("/tutorial-questions/{questionId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> deleteQuestion(@PathVariable Long questionId) {
        return ApiResponse.ok(operations.request("deleteQuestion", new Object[]{questionId}));
    }

    /**
     * 调整顺序问题集合。
     * HTTP 映射：<code>@PutMapping("/tutorial-chapters/{chapterId}/questions/order")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>IdOrderDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PutMapping("/tutorial-chapters/{chapterId}/questions/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> reorderQuestions(@PathVariable Long chapterId, @Valid @RequestBody IdOrderDTO request) {
        return ApiResponse.ok(operations.request("reorderQuestions", new Object[]{chapterId, request.getIds()}));
    }
}
