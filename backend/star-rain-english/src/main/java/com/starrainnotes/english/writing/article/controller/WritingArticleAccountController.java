/*
 * WritingArticleAccountController：英语学习模块的HTTP 接入文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、RevisionDto、WritingArticleService。
 * 阅读入口：list（列出写作Article账号）；get（读取写作Article账号）；create（创建写作Article账号）；save（保存写作Article账号）。
 */

package com.starrainnotes.english.writing.article.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.writing.article.dto.WritingArticleDto.*;
import com.starrainnotes.english.knowledge.dto.RevisionDto;
import com.starrainnotes.english.writing.article.service.WritingArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
/**
 * 写作Article账号控制器，属于英语学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController @RequiredArgsConstructor @RequestMapping("/api/account/english/writing/articles")
public class WritingArticleAccountController {
    /**
     * 业务服务。
     * 协作类型：<code>WritingArticleService</code>。
     */
    private final WritingArticleService service;
    /**
     * 列出写作Article账号。
     * HTTP 映射：<code>@GetMapping</code>。
     * 主要协作调用：<code>service.list</code>。
     *
     * @param search 搜索；类型为 <code>String</code>
     * @param state 状态；类型为 <code>String</code>
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @param genreId genre标识；类型为 <code>Long</code>
     * @param purposeId purpose标识；类型为 <code>Long</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;Page&gt;</code> 的处理结果
     */
    @GetMapping public ApiResponse<Page> list(@RequestParam(required=false) String search,@RequestParam(required=false) String state,@RequestParam(required=false) Long topicId,@RequestParam(required=false) Long genreId,@RequestParam(required=false) Long purposeId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size) { return ApiResponse.ok(service.list(true,search,state,topicId,genreId,purposeId,page,size)); }
    /**
     * 读取写作Article账号。
     * HTTP 映射：<code>@GetMapping("/{id}")</code>。
     * 主要协作调用：<code>service.get</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;Article&gt;</code> 的处理结果
     */
    @GetMapping("/{id}") public ApiResponse<Article> get(@PathVariable String id) { return ApiResponse.ok(service.get(id)); }
    /**
     * 创建写作Article账号。
     * HTTP 映射：<code>@PostMapping</code>。
     * 主要协作调用：<code>service.create</code>。
     *
     * @param r r；类型为 <code>Request</code>
     * @return 类型为 <code>ApiResponse&lt;Article&gt;</code> 的处理结果
     */
    @PostMapping public ApiResponse<Article> create(@RequestBody Request r) { return ApiResponse.ok(service.create(r)); }
    /**
     * 保存写作Article账号。
     * HTTP 映射：<code>@PutMapping("/{id}")</code>。
     * 主要协作调用：<code>service.save</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param r r；类型为 <code>Request</code>
     * @return 类型为 <code>ApiResponse&lt;Article&gt;</code> 的处理结果
     */
    @PutMapping("/{id}") public ApiResponse<Article> save(@PathVariable String id,@RequestBody Request r) { return ApiResponse.ok(service.save(id,r)); }
    /**
     * 完成写作Article账号。
     * HTTP 映射：<code>@PostMapping("/{id}/complete")</code>。
     * 主要协作调用：<code>service.complete</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param r r；类型为 <code>RevisionDto.Request</code>
     * @return 类型为 <code>ApiResponse&lt;Article&gt;</code> 的处理结果
     */
    @PostMapping("/{id}/complete") public ApiResponse<Article> complete(@PathVariable String id,@RequestBody RevisionDto.Request r) { return ApiResponse.ok(service.complete(id,r.getRowVersion())); }
    /**
     * 删除写作Article账号。
     * HTTP 映射：<code>@DeleteMapping("/{id}")</code>。
     * 主要协作调用：<code>service.delete</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable String id) { service.delete(id);return ApiResponse.ok(null); }
    /**
     * 修订集合处理入口。
     * HTTP 映射：<code>@GetMapping("/{id}/revisions")</code>。
     * 主要协作调用：<code>service.revisions</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;RevisionDto.Page&gt;</code> 的处理结果
     */
    @GetMapping("/{id}/revisions") public ApiResponse<RevisionDto.Page> revisions(@PathVariable String id,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size) { return ApiResponse.ok(service.revisions(id,page,size)); }
    /**
     * 修订处理入口。
     * HTTP 映射：<code>@GetMapping("/{id}/revisions/{no}")</code>。
     * 主要协作调用：<code>service.revision</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param no 不含；类型为 <code>long</code>
     * @return 类型为 <code>ApiResponse&lt;RevisionDto.Revision&gt;</code> 的处理结果
     */
    @GetMapping("/{id}/revisions/{no}") public ApiResponse<RevisionDto.Revision> revision(@PathVariable String id,@PathVariable long no) { return ApiResponse.ok(service.revision(id,no)); }
    /**
     * 快照处理入口。
     * HTTP 映射：<code>@PostMapping("/{id}/revisions")</code>。
     * 主要协作调用：<code>service.snapshot</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param r r；类型为 <code>RevisionDto.Request</code>
     * @return 类型为 <code>ApiResponse&lt;RevisionDto.Revision&gt;</code> 的处理结果
     */
    @PostMapping("/{id}/revisions") public ApiResponse<RevisionDto.Revision> snapshot(@PathVariable String id,@RequestBody RevisionDto.Request r) { return ApiResponse.ok(service.snapshot(id,r)); }
    /**
     * 恢复写作Article账号。
     * HTTP 映射：<code>@PostMapping("/{id}/revisions/{no}/restore")</code>。
     * 主要协作调用：<code>service.restore</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param no 不含；类型为 <code>long</code>
     * @param r r；类型为 <code>RevisionDto.Request</code>
     * @return 类型为 <code>ApiResponse&lt;Article&gt;</code> 的处理结果
     */
    @PostMapping("/{id}/revisions/{no}/restore") public ApiResponse<Article> restore(@PathVariable String id,@PathVariable long no,@RequestBody RevisionDto.Request r) { return ApiResponse.ok(service.restore(id,no,r)); }
}
