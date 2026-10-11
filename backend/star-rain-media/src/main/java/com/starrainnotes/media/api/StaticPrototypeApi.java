/*
 * StaticPrototypeApi：媒体模块的跨模块接口文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 阅读入口：validate（校验StaticPrototype）；read（读取StaticPrototype）；contentType（内容类型或请求声明的 Content-Type处理入口）。
 */

package com.starrainnotes.media.api;

/** Static ZIP capability: callers pass asset IDs and relative paths, never storage paths. */
public interface StaticPrototypeApi {
    /**
     * 校验StaticPrototype。
     *
     * @param assetId 资产标识；类型为 <code>Long</code>
     * @param entryPath 入口路径；类型为 <code>String</code>
     */
    void validate(Long assetId, String entryPath);
    /**
     * 读取StaticPrototype。
     *
     * @param assetId 资产标识；类型为 <code>Long</code>
     * @param path 路径；类型为 <code>String</code>
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    byte[] read(Long assetId, String path);
    /**
     * 内容类型或请求声明的 Content-Type处理入口。
     *
     * @param path 路径；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    String contentType(String path);
}
