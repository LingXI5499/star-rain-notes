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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/messages")
public class MessagePublicController {
    private final MessageService service;

    @GetMapping
    public ApiResponse<PageResult<PublicMessageVO>> list(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(service.publicMessages(page, pageSize));
    }

    @PostMapping
    public ApiResponse<MessageSubmissionVO> submit(@RequestBody MessageSubmitDTO request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(new MessageSubmissionVO(service.submit(request, servletRequest.getRemoteAddr()), "PENDING"));
    }
}
