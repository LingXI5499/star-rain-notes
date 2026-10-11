/*
 * ReadingRevisionService：英语学习模块的业务契约文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：Article。
 * 阅读入口：list（列出阅读修订）；get（读取阅读修订）；snapshot（快照处理入口）；restore（恢复阅读修订）。
 */

package com.starrainnotes.english.reading.service;
import com.starrainnotes.english.knowledge.dto.RevisionDto.*;
import com.starrainnotes.english.reading.dto.ReadingDto.Article;
/**
 * 阅读修订业务服务，属于英语学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface ReadingRevisionService {
    /**
     * 列出阅读修订。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>Page</code> 的处理结果
     */
    Page list(String id,int page,int size);
    /**
     * 读取阅读修订。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param no 不含；类型为 <code>long</code>
     * @return 类型为 <code>Revision</code> 的处理结果
     */
    Revision get(String id,long no);
    /**
     * 快照处理入口。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>Revision</code> 的处理结果
     */
    Revision snapshot(String id,Request request);
    /**
     * 恢复阅读修订。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param no 不含；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    Article restore(String id,long no,Request request);
}
