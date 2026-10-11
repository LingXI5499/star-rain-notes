/*
 * ApiException：公共基础模块的业务异常文件。
 * 业务范围：统一响应、异常、分页、安全与跨模块基础契约。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 * 阅读入口：getCode（读取代码）；getStatus（读取状态）。
 */

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
/**
 * 接口异常，属于公共基础模块。
 * 用约定的错误码、公开提示和 HTTP 状态表达业务失败，由统一异常处理器转换为接口响应。
 */
public class ApiException extends RuntimeException {

    /**
     * 代码。
     */
    private final String code;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private final int status;

    /**
     * 构造ApiException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     *
     * @param code 代码；类型为 <code>String</code>
     * @param message 留言；类型为 <code>String</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>int</code>
     */
    public ApiException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    // 带根因的构造器：包装底层异常时必须保留堆栈，否则排障会丢失关键信息
    /**
     * 构造ApiException，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>super</code>。
     *
     * @param code 代码；类型为 <code>String</code>
     * @param message 留言；类型为 <code>String</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>int</code>
     * @param cause 原始异常，用于保留失败原因；类型为 <code>Throwable</code>
     */
    public ApiException(String code, String message, int status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    /**
     * 读取代码。
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String getCode() {
        return code;
    }

    /**
     * 读取状态。
     * @return 类型为 <code>int</code> 的处理结果
     */
    public int getStatus() {
        return status;
    }
}