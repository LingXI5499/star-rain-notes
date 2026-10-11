/*
 * ReadingEnhancementController：英语学习模块的HTTP 接入文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、ReadingEnhancementService。
 * 阅读入口：publicList（公开List处理入口）；adminList（后台List处理入口）；replace（替换阅读Enhancement）；create（创建阅读Enhancement）。
 */

package com.starrainnotes.english.reading.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.reading.dto.ReadingEnhancementDto.*;
import com.starrainnotes.english.reading.service.ReadingEnhancementService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
/**
 * 阅读Enhancement控制器，属于英语学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController @RequiredArgsConstructor
public class ReadingEnhancementController {
    /**
     * 业务服务。
     * 协作类型：<code>ReadingEnhancementService</code>。
     */
    private final ReadingEnhancementService service;
    /**
     * 公开List处理入口。
     * HTTP 映射：<code>@GetMapping("/api/public/english/content/reading/{slug}/{kind:alignments|annotations|vocabulary}")</code>。
     * 主要协作调用：<code>service.list</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param kind kind；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;Item&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/api/public/english/content/reading/{slug}/{kind:alignments|annotations|vocabulary}")
    public ApiResponse<List<Item>> publicList(@PathVariable String slug,@PathVariable String kind) { return ApiResponse.ok(service.list(slug,false,kind)); }
    /**
     * 后台List处理入口。
     * HTTP 映射：<code>@GetMapping("/api/admin/english/content/reading/{id}/{kind:alignments|annotations|vocabulary}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-read-admin\')")</code>。
     * 主要协作调用：<code>service.list</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param kind kind；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;Item&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/api/admin/english/content/reading/{id}/{kind:alignments|annotations|vocabulary}")
    @PreAuthorize("hasAuthority('english:content-read-admin')")
    public ApiResponse<List<Item>> adminList(@PathVariable String id,@PathVariable String kind) { return ApiResponse.ok(service.list(id,true,kind)); }
    /**
     * 替换阅读Enhancement。
     * HTTP 映射：<code>@PutMapping("/api/admin/english/content/reading/{id}/{kind:alignments|annotations|vocabulary}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.replace</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param kind kind；类型为 <code>String</code>
     * @param batch 批量；类型为 <code>Batch</code>
     * @return 类型为 <code>ApiResponse&lt;Result&gt;</code> 的处理结果
     */
    @PutMapping("/api/admin/english/content/reading/{id}/{kind:alignments|annotations|vocabulary}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Result> replace(@PathVariable String id,@PathVariable String kind,@RequestBody Batch batch) { return ApiResponse.ok(service.replace(id,kind,batch)); }
    /**
     * 创建阅读Enhancement。
     * HTTP 映射：<code>@PostMapping("/api/admin/english/content/reading/{id}/{kind:annotations|vocabulary}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.save</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param kind kind；类型为 <code>String</code>
     * @param item item；类型为 <code>Item</code>
     * @return 类型为 <code>ApiResponse&lt;Result&gt;</code> 的处理结果
     */
    @PostMapping("/api/admin/english/content/reading/{id}/{kind:annotations|vocabulary}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Result> create(@PathVariable String id,@PathVariable String kind,@RequestBody Item item) { return ApiResponse.ok(service.save(id,kind,null,item)); }
    /**
     * 更新阅读Enhancement。
     * HTTP 映射：<code>@PutMapping("/api/admin/english/content/reading/{id}/{kind:annotations|vocabulary}/{itemId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.save</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param kind kind；类型为 <code>String</code>
     * @param itemId item标识；类型为 <code>long</code>
     * @param item item；类型为 <code>Item</code>
     * @return 类型为 <code>ApiResponse&lt;Result&gt;</code> 的处理结果
     */
    @PutMapping("/api/admin/english/content/reading/{id}/{kind:annotations|vocabulary}/{itemId}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Result> update(@PathVariable String id,@PathVariable String kind,@PathVariable long itemId,@RequestBody Item item) { return ApiResponse.ok(service.save(id,kind,itemId,item)); }
    /**
     * 删除阅读Enhancement。
     * HTTP 映射：<code>@DeleteMapping("/api/admin/english/content/reading/{id}/{kind:annotations|vocabulary}/{itemId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.delete</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param kind kind；类型为 <code>String</code>
     * @param itemId item标识；类型为 <code>long</code>
     * @param rowVersion 记录版本号，用于区分不同修订或并发更新结果；类型为 <code>long</code>
     * @return 类型为 <code>ApiResponse&lt;Result&gt;</code> 的处理结果
     */
    @DeleteMapping("/api/admin/english/content/reading/{id}/{kind:annotations|vocabulary}/{itemId}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Result> delete(@PathVariable String id,@PathVariable String kind,@PathVariable long itemId,@RequestParam long rowVersion) { return ApiResponse.ok(service.delete(id,kind,itemId,rowVersion)); }
}
