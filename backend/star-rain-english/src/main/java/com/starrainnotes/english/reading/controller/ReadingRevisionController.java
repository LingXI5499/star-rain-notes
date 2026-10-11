/*
 * ReadingRevisionController：英语学习模块的HTTP 接入文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、Article、ReadingRevisionService。
 * 阅读入口：list（列出阅读修订）；get（读取阅读修订）；snapshot（快照处理入口）；restore（恢复阅读修订）。
 */

package com.starrainnotes.english.reading.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.knowledge.dto.RevisionDto.*;
import com.starrainnotes.english.reading.dto.ReadingDto.Article;
import com.starrainnotes.english.reading.service.ReadingRevisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
/**
 * 阅读修订控制器，属于英语学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController @RequiredArgsConstructor @RequestMapping("/api/admin/english/content/reading/{id}/revisions")
@PreAuthorize("hasAuthority('english:content-read-admin')")
public class ReadingRevisionController {
    /**
     * 业务服务。
     * 协作类型：<code>ReadingRevisionService</code>。
     */
    private final ReadingRevisionService service;
    /**
     * 列出阅读修订。
     * HTTP 映射：<code>@GetMapping</code>。
     * 主要协作调用：<code>service.list</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;Page&gt;</code> 的处理结果
     */
    @GetMapping public ApiResponse<Page> list(@PathVariable String id,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size) { return ApiResponse.ok(service.list(id,page,size)); }
    /**
     * 读取阅读修订。
     * HTTP 映射：<code>@GetMapping("/{no}")</code>。
     * 主要协作调用：<code>service.get</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param no 不含；类型为 <code>long</code>
     * @return 类型为 <code>ApiResponse&lt;Revision&gt;</code> 的处理结果
     */
    @GetMapping("/{no}") public ApiResponse<Revision> get(@PathVariable String id,@PathVariable long no) { return ApiResponse.ok(service.get(id,no)); }
    /**
     * 快照处理入口。
     * HTTP 映射：<code>@PostMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.snapshot</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>ApiResponse&lt;Revision&gt;</code> 的处理结果
     */
    @PostMapping @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Revision> snapshot(@PathVariable String id,@RequestBody Request request) { return ApiResponse.ok(service.snapshot(id,request)); }
    /**
     * 恢复阅读修订。
     * HTTP 映射：<code>@PostMapping("/{no}/restore")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.restore</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param no 不含；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>ApiResponse&lt;Article&gt;</code> 的处理结果
     */
    @PostMapping("/{no}/restore") @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Article> restore(@PathVariable String id,@PathVariable long no,@RequestBody Request request) { return ApiResponse.ok(service.restore(id,no,request)); }
}
