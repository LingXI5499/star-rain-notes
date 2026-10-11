/*
 * MessageAdminController：留言模块的HTTP 接入文件。
 * 业务范围：留言提交、审核展示和后台管理。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、PageResult、MessageRejectDTO、MessageService、AdminMessageVO、MessageActionVO。
 * 阅读入口：list（列出留言后台）；pending（待处理处理入口）；actions（actions处理入口）；approve（批准留言后台）。
 */

package com.starrainnotes.message.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.message.dto.MessageRejectDTO;
import com.starrainnotes.message.service.MessageService;
import com.starrainnotes.message.vo.AdminMessageVO;
import com.starrainnotes.message.vo.MessageActionVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 留言后台控制器，属于留言模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/messages")
public class MessageAdminController {
    /**
     * 业务服务。
     * 协作类型：<code>MessageService</code>。
     */
    private final MessageService service;

    /**
     * 列出留言后台。
     * HTTP 映射：<code>@GetMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'message:read-admin\')")</code>。
     * 主要协作调用：<code>service.adminMessages</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;AdminMessageVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('message:read-admin')")
    public ApiResponse<PageResult<AdminMessageVO>> list(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int pageSize,
                                                         @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.adminMessages(page, pageSize, status));
    }

    /**
     * 待处理处理入口。
     * HTTP 映射：<code>@GetMapping("/pending")</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'message:read-admin\')")</code>。
     * 主要协作调用：<code>service.adminMessages</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;AdminMessageVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('message:read-admin')")
    public ApiResponse<PageResult<AdminMessageVO>> pending(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(service.adminMessages(page, pageSize, "PENDING"));
    }

    /**
     * actions处理入口。
     * HTTP 映射：<code>@GetMapping("/{id}/actions")</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'message:read-admin\')")</code>。
     * 主要协作调用：<code>service.actions</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;MessageActionVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/{id}/actions")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('message:read-admin')")
    public ApiResponse<List<MessageActionVO>> actions(@PathVariable long id) {
        return ApiResponse.ok(service.actions(id));
    }

    /**
     * 批准留言后台。
     * HTTP 映射：<code>@PostMapping("/{id}/approve")</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'message:moderate\')")</code>。
     * 主要协作调用：<code>service.approve</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>ApiResponse&lt;AdminMessageVO&gt;</code> 的处理结果
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('message:moderate')")
    public ApiResponse<AdminMessageVO> approve(@PathVariable long id) {
        return ApiResponse.ok(service.approve(id));
    }

    /**
     * 拒绝留言后台。
     * HTTP 映射：<code>@PostMapping("/{id}/reject")</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'message:moderate\')")</code>。
     * 主要协作调用：<code>service.reject</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>MessageRejectDTO</code>
     * @return 类型为 <code>ApiResponse&lt;AdminMessageVO&gt;</code> 的处理结果
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('message:moderate')")
    public ApiResponse<AdminMessageVO> reject(@PathVariable long id, @RequestBody MessageRejectDTO request) {
        return ApiResponse.ok(service.reject(id, request == null ? null : request.getReason()));
    }

    /**
     * hide处理入口。
     * HTTP 映射：<code>@PostMapping("/{id}/hide")</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'message:hide\')")</code>。
     * 主要协作调用：<code>service.hide</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>ApiResponse&lt;AdminMessageVO&gt;</code> 的处理结果
     */
    @PostMapping("/{id}/hide")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('message:hide')")
    public ApiResponse<AdminMessageVO> hide(@PathVariable long id) {
        return ApiResponse.ok(service.hide(id));
    }

    /**
     * 恢复留言后台。
     * HTTP 映射：<code>@PostMapping("/{id}/restore")</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'message:hide\')")</code>。
     * 主要协作调用：<code>service.restore</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>ApiResponse&lt;AdminMessageVO&gt;</code> 的处理结果
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('message:hide')")
    public ApiResponse<AdminMessageVO> restore(@PathVariable long id) {
        return ApiResponse.ok(service.restore(id));
    }

    /**
     * 删除留言后台。
     * HTTP 映射：<code>@DeleteMapping("/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'message:delete\')")</code>。
     * 主要协作调用：<code>service.delete</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('message:delete')")
    public ApiResponse<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
