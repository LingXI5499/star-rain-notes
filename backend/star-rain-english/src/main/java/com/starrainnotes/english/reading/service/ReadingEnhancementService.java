/*
 * ReadingEnhancementService：英语学习模块的业务契约文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 阅读入口：list（列出阅读Enhancement）；replace（替换阅读Enhancement）；save（保存阅读Enhancement）；delete（删除阅读Enhancement）。
 */

package com.starrainnotes.english.reading.service;
import com.starrainnotes.english.reading.dto.ReadingEnhancementDto.*;
import java.util.List;
/**
 * 阅读Enhancement业务服务，属于英语学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface ReadingEnhancementService {
    /**
     * 列出阅读Enhancement。
     *
     * @param identity 身份；类型为 <code>String</code>
     * @param admin 后台；类型为 <code>boolean</code>
     * @param kind kind；类型为 <code>String</code>
     * @return 本次处理得到的条目集合
     */
    List<Item> list(String identity,boolean admin,String kind);
    /**
     * 替换阅读Enhancement。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param kind kind；类型为 <code>String</code>
     * @param batch 批量；类型为 <code>Batch</code>
     * @return 类型为 <code>Result</code> 的处理结果
     */
    Result replace(String id,String kind,Batch batch);
    /**
     * 保存阅读Enhancement。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param kind kind；类型为 <code>String</code>
     * @param itemId item标识；类型为 <code>Long</code>
     * @param item item；类型为 <code>Item</code>
     * @return 类型为 <code>Result</code> 的处理结果
     */
    Result save(String id,String kind,Long itemId,Item item);
    /**
     * 删除阅读Enhancement。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param kind kind；类型为 <code>String</code>
     * @param itemId item标识；类型为 <code>long</code>
     * @param version 版本；类型为 <code>long</code>
     * @return 类型为 <code>Result</code> 的处理结果
     */
    Result delete(String id,String kind,long itemId,long version);
    /**
     * 恢复阅读Enhancement。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param snapshot 快照；类型为 <code>Snapshot</code>
     */
    void restore(long id,Snapshot snapshot);
}
