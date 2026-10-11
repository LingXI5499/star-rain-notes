/*
 * ApiResponse：公共基础模块的模块支持文件。
 * 业务范围：统一响应、异常、分页、安全与跨模块基础契约。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 阅读入口：ok（ok处理入口）。
 */

package com.starrainnotes.common.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口响应，属于公共基础模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 代码。
     */
    private String code;
    /**
     * 留言。
     */
    private String message;
    /**
     * data。
     */
    private T data;

    /**
     * ok处理入口。
     * 关键处理步骤调用 <code>ApiResponse.builder</code>。
     *
     * @param data data；类型为 <code>T</code>
     * @return 类型为 <code>ApiResponse&lt;T&gt;</code> 的处理结果
     */
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .code("SUCCESS")
                .message("ok")
                .data(data)
                .build();
    }
}
