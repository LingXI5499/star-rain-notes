package com.starrainnotes.common.exception;

/*
 * 业务异常基类。
 *
 * 全站所有可预期的业务错误都继承它：基类持有 错误码 / 用户可读消息 / HTTP 状态，
 * 每个具体错误一个子类，这样调用方既能按类型 catch，也能统一由
 * GlobalApiExceptionHandler 输出成同一套 ApiResponse 结构。
 *
 * 不要再用「集中式静态工厂 + 直接 new 基类」的写法，那会丢掉类型语义。
 */
public class ApiException extends RuntimeException {

    private final String code;
    private final int status;

    public ApiException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    // 带根因的构造器：包装底层异常时必须保留堆栈，否则排障会丢失关键信息
    public ApiException(String code, String message, int status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}