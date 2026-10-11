/*
 * GlobalApiExceptionHandler：公共基础模块的模块支持文件。
 * 业务范围：统一响应、异常、分页、安全与跨模块基础契约。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：ApiException、ApiResponse。
 * 阅读入口：business（business处理入口）；invalidRequest（无效请求处理入口）；unauthorized（unauthorized处理入口）；payloadTooLarge（提交内容TooLarge处理入口）。
 */

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
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global接口异常处理器，属于公共基础模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@RestControllerAdvice
public class GlobalApiExceptionHandler {

    /**
     * 当前类的日志记录器；日志参数不能包含密码或会话令牌。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

    /**
     * business处理入口。
     * 关键处理步骤调用 <code>ResponseEntity.status</code>、<code>exception.getStatus</code>、<code>exception.getCode</code>、<code>exception.getMessage</code>。
     *
     * @param exception 异常；类型为 <code>ApiException</code>
     * @return 类型为 <code>ResponseEntity&lt;ApiResponse&lt;Void&gt;&gt;</code> 的处理结果
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> business(ApiException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiResponse<>(exception.getCode(), exception.getMessage(), null));
    }

    /**
     * 无效请求处理入口。
     * 关键处理步骤调用 <code>ResponseEntity.badRequest</code>。
     *
     * @param exception 异常；类型为 <code>Exception</code>
     * @return 类型为 <code>ResponseEntity&lt;ApiResponse&lt;Void&gt;&gt;</code> 的处理结果
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> invalidRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>("INVALID_REQUEST", "请求参数无效", null));
    }

    /**
     * unauthorized处理入口。
     * 关键处理步骤调用 <code>ResponseEntity.status</code>。
     *
     * @param exception 异常；类型为 <code>AuthenticationException</code>
     * @return 类型为 <code>ResponseEntity&lt;ApiResponse&lt;Void&gt;&gt;</code> 的处理结果
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> unauthorized(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>("UNAUTHORIZED", "请先登录", null));
    }

    // 触发 multipart 硬上限时抛出，位置早于业务模块自己的上传校验
    /**
     * 提交内容TooLarge处理入口。
     * 关键处理步骤调用 <code>ResponseEntity.status</code>。
     *
     * @param exception 异常；类型为 <code>MaxUploadSizeExceededException</code>
     * @return 类型为 <code>ResponseEntity&lt;ApiResponse&lt;Void&gt;&gt;</code> 的处理结果
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> payloadTooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiResponse<>("PAYLOAD_TOO_LARGE", "上传内容超过服务器允许的最大体积", null));
    }

    /**
     * 禁止公开处理入口。
     * 关键处理步骤调用 <code>ResponseEntity.status</code>。
     *
     * @param exception 异常；类型为 <code>AccessDeniedException</code>
     * @return 类型为 <code>ResponseEntity&lt;ApiResponse&lt;Void&gt;&gt;</code> 的处理结果
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> forbidden(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>("FORBIDDEN", "没有操作权限", null));
    }

    /*
     * Spring Security 6 的 @PreAuthorize 拒绝抛出的是 AuthorizationDeniedException，
     * 它是 AccessDeniedException 的子类，但方法级安全发生在 DispatcherServlet 之外，
     * 由 ExceptionHandlerExceptionResolver 直接解析，因此必须显式声明这个更具体的类型。
     * 漏掉它会让「没有权限」变成 500，前端就无法区分越权与服务器故障。
     * 只有确实被声明过的 URL 访问边界才需要这里兜底：URL 层的 403 由安全链自己输出。
     */
    /**
     * authorization被拒绝处理入口。
     * 关键处理步骤调用 <code>ResponseEntity.status</code>。
     *
     * @param exception 异常；类型为 <code>AuthorizationDeniedException</code>
     * @return 类型为 <code>ResponseEntity&lt;ApiResponse&lt;Void&gt;&gt;</code> 的处理结果
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> authorizationDenied(AuthorizationDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>("FORBIDDEN", "没有操作权限", null));
    }

    /**
     * 不存在处理入口。
     * 关键处理步骤调用 <code>ResponseEntity.status</code>。
     *
     * @param exception 异常；类型为 <code>Exception</code>
     * @return 类型为 <code>ResponseEntity&lt;ApiResponse&lt;Void&gt;&gt;</code> 的处理结果
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> notFound(Exception exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>("NOT_FOUND", "页面或接口不存在", null));
    }

    /**
     * unexpected处理入口。
     * 关键处理步骤调用 <code>ResponseEntity.status</code>。
     *
     * @param exception 异常；类型为 <code>Exception</code>
     * @return 类型为 <code>ResponseEntity&lt;ApiResponse&lt;Void&gt;&gt;</code> 的处理结果
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception exception) {
        LOGGER.error("Unhandled API error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>("INTERNAL_ERROR", "服务暂时不可用", null));
    }
}
