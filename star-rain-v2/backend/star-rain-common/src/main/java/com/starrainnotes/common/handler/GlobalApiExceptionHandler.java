package com.starrainnotes.common.handler;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.common.result.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> business(ApiException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiResponse<>(exception.getCode(), exception.getMessage(), null));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> invalidRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>("INVALID_REQUEST", "请求参数无效", null));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> unauthorized(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>("UNAUTHORIZED", "请先登录", null));
    }

    // 触发 multipart 硬上限时抛出，位置早于业务模块自己的上传校验
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> payloadTooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiResponse<>("PAYLOAD_TOO_LARGE", "上传内容超过服务器允许的最大体积", null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> forbidden(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>("FORBIDDEN", "没有操作权限", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception exception) {
        LOGGER.error("Unhandled API error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>("INTERNAL_ERROR", "服务暂时不可用", null));
    }
}
