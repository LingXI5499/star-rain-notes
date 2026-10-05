package com.starrainnotes.media.storage;

import com.starrainnotes.media.storage.StorageWriteCommand;
import com.starrainnotes.media.storage.StoredObject;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;

/*
 * 媒体字节的存储抽象。
 *
 * 业务 Service 不允许散落 Files.write() / Files.delete()，
 * 也不允许把服务器绝对路径写进数据库。
 *
 * 当前实现是 LocalFileMediaStorage；未来可增加 MinIO / OSS / S3 实现，
 * 业务层与数据库字段都不需要改。Storage 不承担业务权限判断，
 * 也不理解任何业务模块的状态机。
 */
public interface MediaStorage {

    // 存储提供方标识，写入 sr_media_asset.storage_provider
    String provider();

    /*
     * 生成 storageKey 并落盘。
     * 入参是流而不是字节数组：视频可达 200MB，不能整份读进堆内存。
     * 已存在的 key 不得被覆盖。
     */
    StoredObject store(StorageWriteCommand command);

    // 打开完整内容；对象缺失时抛 MEDIA_CONTENT_MISSING
    InputStream open(String storageKey);

    /*
     * 按字节区间打开，供音频/视频拖动进度与断点续传使用。
     * start 从 0 开始，length 为要读取的字节数。
     */
    InputStream openRange(String storageKey, long start, long length);

    boolean exists(String storageKey);

    long size(String storageKey);

    void delete(String storageKey);

    // 对象最后写入时间；对象不存在时返回 null。供孤儿文件宽限期判断使用
    Instant lastModified(String storageKey);

    // 列出全部 storageKey，供孤儿文件清理使用
    List<String> listKeys();
}
