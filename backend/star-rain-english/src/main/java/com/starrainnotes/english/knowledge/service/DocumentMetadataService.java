/*
 * DocumentMetadataService：英语学习模块的业务契约文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：DocumentMetadata。
 * 阅读入口：validate（校验文档元数据）；validateSnapshot（校验快照）；replaceSnapshot（替换快照）；load（加载文档元数据）。
 */

package com.starrainnotes.english.knowledge.service;
import com.starrainnotes.english.knowledge.dto.DocumentMetadata;
import com.starrainnotes.english.knowledge.dto.RevisionDto.*;
/**
 * 文档元数据业务服务，属于英语学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface DocumentMetadataService {
    /**
     * 校验文档元数据。
     *
     * @param value 值；类型为 <code>DocumentMetadata</code>
     */
    void validate(DocumentMetadata value);
    /**
     * 校验快照。
     *
     * @param value 值；类型为 <code>DocumentMetadata</code>
     */
    void validateSnapshot(DocumentMetadata value);
    /**
     * 替换快照。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param value 值；类型为 <code>DocumentMetadata</code>
     */
    void replaceSnapshot(String kind,long id,DocumentMetadata value);
    /**
     * 加载文档元数据。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param value 值；类型为 <code>DocumentMetadata</code>
     */
    void load(String kind,long id,DocumentMetadata value);
    /**
     * 替换文档元数据。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param value 值；类型为 <code>DocumentMetadata</code>
     */
    void replace(String kind,long id,DocumentMetadata value);
    /**
     * 修订集合处理入口。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>Page</code> 的处理结果
     */
    Page revisions(String kind,long id,int page,int size);
    /**
     * 修订处理入口。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param no 不含；类型为 <code>long</code>
     * @return 类型为 <code>Revision</code> 的处理结果
     */
    Revision revision(String kind,long id,long no);
    /**
     * 快照处理入口。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param actor 当前操作对应的身份对象；类型为 <code>long</code>
     * @param snapshot 快照；类型为 <code>Object</code>
     * @param note note；类型为 <code>String</code>
     * @return 类型为 <code>Revision</code> 的处理结果
     */
    Revision snapshot(String kind,long id,long actor,Object snapshot,String note);
}
