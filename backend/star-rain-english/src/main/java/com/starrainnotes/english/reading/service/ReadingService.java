/*
 * ReadingService：英语学习模块的业务契约文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 阅读入口：list（列出阅读）；listFiltered（列出Filtered）；get（读取阅读）；create（创建阅读）。
 */

package com.starrainnotes.english.reading.service;
import com.starrainnotes.english.reading.dto.ReadingDto.*;
/**
 * 阅读业务服务，属于英语学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface ReadingService {
    /**
     * 列出阅读。
     *
     * @param admin 后台；类型为 <code>boolean</code>
     * @param search 搜索；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>Page</code> 的处理结果
     */
    Page list(boolean admin,String search,int page,int size);
    /**
     * 列出Filtered。
     *
     * @param admin 后台；类型为 <code>boolean</code>
     * @param search 搜索；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @param genreId genre标识；类型为 <code>Long</code>
     * @param purposeId purpose标识；类型为 <code>Long</code>
     * @return 类型为 <code>Page</code> 的处理结果
     */
    Page listFiltered(boolean admin,String search,int page,int size,Long topicId,Long genreId,Long purposeId);
    /**
     * 读取阅读。
     *
     * @param identity 身份；类型为 <code>String</code>
     * @param admin 后台；类型为 <code>boolean</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article get(String identity,boolean admin);
    /**
     * 创建阅读。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article create(Request request);
    /**
     * 更新阅读。
     *
     * @param identity 身份；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article update(String identity,Request request);
    /**
     * 恢复快照。
     *
     * @param identity 身份；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article restoreSnapshot(String identity,Request request);
    /**
     * 设置已发布。
     *
     * @param identity 身份；类型为 <code>String</code>
     * @param published 已发布；类型为 <code>boolean</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article setPublished(String identity,boolean published);
    /**
     * 设置已发布。
     *
     * @param identity 身份；类型为 <code>String</code>
     * @param published 已发布；类型为 <code>boolean</code>
     * @param expectedVersion expected版本；类型为 <code>Long</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article setPublished(String identity,boolean published,Long expectedVersion);
    /**
     * 删除阅读。
     *
     * @param identity 身份；类型为 <code>String</code>
     */
    void delete(String identity);
}
