/*
 * WritingArticleService：英语学习模块的业务契约文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：RevisionDto。
 * 阅读入口：list（列出写作Article）；get（读取写作Article）；getPublic（读取公开）；create（创建写作Article）。
 */

package com.starrainnotes.english.writing.article.service;
import com.starrainnotes.english.writing.article.dto.WritingArticleDto.*;
import com.starrainnotes.english.knowledge.dto.RevisionDto;
/**
 * 写作Article业务服务，属于英语学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface WritingArticleService {
    /**
     * 列出写作Article。
     *
     * @param mine mine；类型为 <code>boolean</code>
     * @param search 搜索；类型为 <code>String</code>
     * @param state 状态；类型为 <code>String</code>
     * @param topic 专题；类型为 <code>Long</code>
     * @param genre genre；类型为 <code>Long</code>
     * @param purpose purpose；类型为 <code>Long</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>Page</code> 的处理结果
     */
    Page list(boolean mine,String search,String state,Long topic,Long genre,Long purpose,int page,int size);
    /**
     * 读取写作Article。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article get(String id);
    /**
     * 读取公开。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article getPublic(String slug);
    /**
     * 创建写作Article。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article create(Request request);
    /**
     * 保存写作Article。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article save(String id,Request request);
    /**
     * 完成写作Article。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param version 版本；类型为 <code>Long</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article submit(String id,Long version);
    Article cancelReview(String id,Long version);
    Article complete(String id,Long version);
    /**
     * 发布写作Article。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param version 版本；类型为 <code>Long</code>
     * @param publish 发布；类型为 <code>boolean</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article publish(String id,Long version,boolean publish);
    /**
     * 删除写作Article。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     */
    void delete(String id);
    /**
     * 修订集合处理入口。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>RevisionDto.Page</code> 的处理结果
     */
    RevisionDto.Page revisions(String id,int page,int size);
    /**
     * 修订处理入口。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param no 不含；类型为 <code>long</code>
     * @return 类型为 <code>RevisionDto.Revision</code> 的处理结果
     */
    RevisionDto.Revision revision(String id,long no);
    /**
     * 快照处理入口。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>RevisionDto.Request</code>
     * @return 类型为 <code>RevisionDto.Revision</code> 的处理结果
     */
    RevisionDto.Revision snapshot(String id,RevisionDto.Request request);
    /**
     * 恢复写作Article。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param no 不含；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>RevisionDto.Request</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article restore(String id,long no,RevisionDto.Request request);
}
