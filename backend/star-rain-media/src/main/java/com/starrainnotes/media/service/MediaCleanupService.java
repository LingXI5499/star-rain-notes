/*
 * MediaCleanupService：媒体模块的业务契约文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 阅读入口：cleanupOrphans（清理Orphans）。
 */

package com.starrainnotes.media.service;

/*
 * 媒体存储侧的后台清理能力。
 *
 * 存在的理由：文件系统与 MySQL 不在同一事务里。上传流程已经做了
 * “写库失败就删文件”的补偿，但如果进程在落盘之后、写库之前崩溃，
 * 就会留下永远没有数据库记录的垃圾文件。
 */
/**
 * 媒体清理业务服务，属于媒体模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface MediaCleanupService {

    // 清理无数据库记录、且已超过宽限期的孤儿文件；返回实际删除的数量
    /**
     * 清理Orphans。
     * @return 类型为 <code>int</code> 的处理结果
     */
    int cleanupOrphans();
}
