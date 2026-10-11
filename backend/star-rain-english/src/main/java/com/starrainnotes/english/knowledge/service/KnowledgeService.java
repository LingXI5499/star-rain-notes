/*
 * KnowledgeService：英语学习模块的业务契约文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：Page。
 * 阅读入口：list（列出知识）。
 */

package com.starrainnotes.english.knowledge.service;
import com.starrainnotes.english.knowledge.dto.KnowledgeDto.Page;
/**
 * 知识业务服务，属于英语学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface KnowledgeService {
    /**
     * 列出知识。
     *
     * @param type 类型；类型为 <code>String</code>
     * @param search 搜索；类型为 <code>String</code>
     * @param topic 专题；类型为 <code>Long</code>
     * @param genre genre；类型为 <code>Long</code>
     * @param purpose purpose；类型为 <code>Long</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>Page</code> 的处理结果
     */
    Page list(String type,String search,Long topic,Long genre,Long purpose,int page,int size); }
