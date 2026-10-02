package com.starrainnotes.media.service;

/*
 * 媒体存储侧的后台清理能力。
 *
 * 存在的理由：文件系统与 MySQL 不在同一事务里。上传流程已经做了
 * “写库失败就删文件”的补偿，但如果进程在落盘之后、写库之前崩溃，
 * 就会留下永远没有数据库记录的垃圾文件。
 */
public interface MediaCleanupService {

    // 清理无数据库记录、且已超过宽限期的孤儿文件；返回实际删除的数量
    int cleanupOrphans();
}
