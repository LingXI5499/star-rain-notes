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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/messages")
public class MessageAdminController {
    private final MessageService service;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('message:read-admin')")
    public ApiResponse<PageResult<AdminMessageVO>> list(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int pageSize,
                                                         @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.adminMessages(page, pageSize, status));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('message:read-admin')")
    public ApiResponse<PageResult<AdminMessageVO>> pending(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(service.adminMessages(page, pageSize, "PENDING"));
    }

    @GetMapping("/{id}/actions")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('message:read-admin')")
    public ApiResponse<List<MessageActionVO>> actions(@PathVariable long id) {
        return ApiResponse.ok(service.actions(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('message:moderate')")
    public ApiResponse<AdminMessageVO> approve(@PathVariable long id) {
        return ApiResponse.ok(service.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('message:moderate')")
    public ApiResponse<AdminMessageVO> reject(@PathVariable long id, @RequestBody MessageRejectDTO request) {
        return ApiResponse.ok(service.reject(id, request == null ? null : request.getReason()));
    }

    @PostMapping("/{id}/hide")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('message:hide')")
    public ApiResponse<AdminMessageVO> hide(@PathVariable long id) {
        return ApiResponse.ok(service.hide(id));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('message:hide')")
    public ApiResponse<AdminMessageVO> restore(@PathVariable long id) {
        return ApiResponse.ok(service.restore(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('message:delete')")
    public ApiResponse<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
