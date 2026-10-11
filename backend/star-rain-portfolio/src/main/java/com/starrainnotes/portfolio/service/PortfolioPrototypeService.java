/*
 * PortfolioPrototypeService：作品模块的业务契约文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：WorkPrototypeDTO。
 * 阅读入口：bind（绑定作品Prototype）；read（读取作品Prototype）；contentType（内容类型或请求声明的 Content-Type处理入口）。
 */

package com.starrainnotes.portfolio.service;
import com.starrainnotes.portfolio.dto.WorkPrototypeDTO;
/**
 * 作品Prototype业务服务，属于作品模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface PortfolioPrototypeService {
    /**
     * 绑定作品Prototype。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkPrototypeDTO</code>
     */
    void bind(Long workId, WorkPrototypeDTO request);
    /**
     * 读取作品Prototype。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param path 路径；类型为 <code>String</code>
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    byte[] read(String slug, String path);
    /**
     * 内容类型或请求声明的 Content-Type处理入口。
     *
     * @param path 路径；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    String contentType(String path);
}
