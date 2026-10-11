/*
 * MessagePublicController：留言模块的HTTP 接入文件。
 * 业务范围：留言提交、审核展示和后台管理。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、PageResult、MessageSubmitDTO、MessageService、PublicMessageVO、MessageSubmissionVO。
 * 阅读入口：list（列出留言公开）；submit（提交留言公开）。
 */

package com.starrainnotes.message.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.message.dto.MessageSubmitDTO;
import com.starrainnotes.message.service.MessageService;
import com.starrainnotes.message.vo.PublicMessageVO;
import com.starrainnotes.message.vo.MessageSubmissionVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 留言公开控制器，属于留言模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/messages")
public class MessagePublicController {
    /**
     * 业务服务。
     * 协作类型：<code>MessageService</code>。
     */
    private final MessageService service;

    /**
     * 列出留言公开。
     * HTTP 映射：<code>@GetMapping</code>。
     * 主要协作调用：<code>service.publicMessages</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;PublicMessageVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping
    public ApiResponse<PageResult<PublicMessageVO>> list(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(service.publicMessages(page, pageSize));
    }

    /**
     * 提交留言公开。
     * HTTP 映射：<code>@PostMapping</code>。
     * 主要协作调用：<code>service.submit</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>MessageSubmitDTO</code>
     * @param servletRequest servlet请求；类型为 <code>HttpServletRequest</code>
     * @return 类型为 <code>ApiResponse&lt;MessageSubmissionVO&gt;</code> 的处理结果
     */
    @PostMapping
    public ApiResponse<MessageSubmissionVO> submit(@RequestBody MessageSubmitDTO request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(new MessageSubmissionVO(service.submit(request, servletRequest.getRemoteAddr()), "PENDING"));
    }
}
