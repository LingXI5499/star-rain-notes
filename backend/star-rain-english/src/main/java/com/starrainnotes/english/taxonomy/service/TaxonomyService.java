/*
 * TaxonomyService：英语学习模块的业务契约文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：Node。
 * 阅读入口：tree（tree处理入口）；get（读取Taxonomy）；validateIds（校验标识集合）；validateSnapshotIds（校验快照标识集合）。
 */

package com.starrainnotes.english.taxonomy.service;
import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import java.util.List;
/**
 * Taxonomy业务服务，属于英语学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface TaxonomyService {
    /**
     * tree处理入口。
     *
     * @param admin 后台；类型为 <code>boolean</code>
     * @return 本次处理得到的条目集合
     */
    List<Node> tree(boolean admin);
    /**
     * 读取Taxonomy。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>Node</code> 的处理结果
     */
    Node get(String slug);
    /**
     * 校验标识集合。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @param dimension dimension；类型为 <code>String</code>
     * @return 本次处理得到的条目集合
     */
    List<Long> validateIds(List<Long> ids,String dimension);
    /**
     * 校验快照标识集合。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @param dimension dimension；类型为 <code>String</code>
     * @return 本次处理得到的条目集合
     */
    List<Long> validateSnapshotIds(List<Long> ids,String dimension);
    /**
     * descendants处理入口。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param dimension dimension；类型为 <code>String</code>
     * @return 本次处理得到的条目集合
     */
    List<Long> descendants(Long id,String dimension);
    /**
     * 创建Taxonomy。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>Node</code>
     * @return 类型为 <code>Node</code> 的处理结果
     */
    Node create(Node request);
    /**
     * 更新Taxonomy。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Node</code>
     * @return 类型为 <code>Node</code> 的处理结果
     */
    Node update(long id,Node request);
    /**
     * 禁用Taxonomy。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     */
    void disable(long id);
}
