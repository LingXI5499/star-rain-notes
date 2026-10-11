/*
 * ReadingAdminController：英语学习模块的HTTP 接入文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、Article、Page、Request、ReadingService。
 * 阅读入口：list（列出阅读后台）；get（读取阅读后台）；create（创建阅读后台）；update（更新阅读后台）。
 */

package com.starrainnotes.english.reading.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.reading.dto.ReadingDto.Article;
import com.starrainnotes.english.reading.dto.ReadingDto.Page;
import com.starrainnotes.english.reading.dto.ReadingDto.Request;
import com.starrainnotes.english.reading.service.ReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 阅读后台控制器，属于英语学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/english/content/reading")
@PreAuthorize("hasAuthority('english:content-read-admin')")
public class ReadingAdminController {
    /**
     * 业务服务。
     * 协作类型：<code>ReadingService</code>。
     */
    private final ReadingService service;

    /**
     * 列出阅读后台。
     * HTTP 映射：<code>@GetMapping</code>。
     * 主要协作调用：<code>service.listFiltered</code>。
     *
     * @param search 搜索；类型为 <code>String</code>
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @param genreId genre标识；类型为 <code>Long</code>
     * @param purposeId purpose标识；类型为 <code>Long</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;Page&gt;</code> 的处理结果
     */
    @GetMapping
    public ApiResponse<Page> list(@RequestParam(required = false) String search,
                                  @RequestParam(required=false) Long topicId, @RequestParam(required=false) Long genreId, @RequestParam(required=false) Long purposeId,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listFiltered(true, search, page, size, topicId, genreId, purposeId));
    }

    /**
     * 读取阅读后台。
     * HTTP 映射：<code>@GetMapping("/{id}")</code>。
     * 主要协作调用：<code>service.get</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;Article&gt;</code> 的处理结果
     */
    @GetMapping("/{id}")
    public ApiResponse<Article> get(@PathVariable String id) { return ApiResponse.ok(service.get(id, true)); }

    /**
     * 创建阅读后台。
     * HTTP 映射：<code>@PostMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.create</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>ApiResponse&lt;Article&gt;</code> 的处理结果
     */
    @PostMapping
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Article> create(@RequestBody Request request) { return ApiResponse.ok(service.create(request)); }

    /**
     * 更新阅读后台。
     * HTTP 映射：<code>@PutMapping("/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.update</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>ApiResponse&lt;Article&gt;</code> 的处理结果
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Article> update(@PathVariable String id, @RequestBody Request request) {
        return ApiResponse.ok(service.update(id, request));
    }

    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束处理入口。
     * HTTP 映射：<code>@PostMapping("/{id}/{action:publish|withdraw}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.setPublished</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param action 动作；类型为 <code>String</code>
     * @param version 版本；类型为 <code>com.starrainnotes.english.knowledge.dto.RevisionDto.Request</code>
     * @return 类型为 <code>ApiResponse&lt;Article&gt;</code> 的处理结果
     */
    @PostMapping("/{id}/{action:publish|withdraw}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Article> status(@PathVariable String id, @PathVariable String action, @RequestBody(required=false) com.starrainnotes.english.knowledge.dto.RevisionDto.Request version) {
        return ApiResponse.ok(service.setPublished(id, "publish".equals(action), version==null?null:version.getRowVersion()));
    }

    /**
     * 删除阅读后台。
     * HTTP 映射：<code>@DeleteMapping("/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.delete</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
