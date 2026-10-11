/*
 * MediaStorage：媒体模块的存储适配文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 实现文件或存储访问边界，向业务服务提供读写能力，并将底层失败转换为本模块约定的异常。
 * 主要协作类型：MediaStorageResultDTO。
 * 阅读入口：provider（提供方处理入口）；store（存储媒体存储）；open（打开媒体存储）；openRange（打开范围）。
 */

package com.starrainnotes.media.storage;

import com.starrainnotes.media.dto.MediaStorageResultDTO;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;

/*
 * 媒体字节的存储抽象。
 *
 * 业务 Service 不允许散落 Files.write() / Files.delete()，
 * 也不允许把服务器绝对路径写进数据库。
 *
 * 当前实现是 LocalFileMediaStorageImpl；未来可增加 MinIO / OSS / S3 实现，
 * 业务层与数据库字段都不需要改。Storage 不承担业务权限判断，
 * 也不理解任何业务模块的状态机。
 */
/**
 * 媒体存储，属于媒体模块。
 * 实现文件或存储访问边界，向业务服务提供读写能力，并将底层失败转换为本模块约定的异常。
 */
public interface MediaStorage {

    // 存储提供方标识，写入 sr_media_asset.storage_provider
    /**
     * 提供方处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    String provider();

    /*
     * 生成 storageKey 并落盘。
     * 入参是流而不是字节数组：视频可达 200MB，不能整份读进堆内存。
     * 已存在的 key 不得被覆盖。
     */
    /**
     * 存储媒体存储。
     *
     * @param content 内容；类型为 <code>InputStream</code>
     * @param fileExtension 文件扩展名；类型为 <code>String</code>
     * @return 类型为 <code>MediaStorageResultDTO</code> 的处理结果
     */
    MediaStorageResultDTO store(InputStream content, String fileExtension);

    // 打开完整内容；对象缺失时抛 MEDIA_CONTENT_MISSING
    /**
     * 打开媒体存储。
     *
     * @param storageKey 媒体在存储根目录内的相对键；类型为 <code>String</code>
     * @return 类型为 <code>InputStream</code> 的处理结果
     */
    InputStream open(String storageKey);

    /*
     * 按字节区间打开，供音频/视频拖动进度与断点续传使用。
     * start 从 0 开始，length 为要读取的字节数。
     */
    /**
     * 打开范围。
     *
     * @param storageKey 媒体在存储根目录内的相对键；类型为 <code>String</code>
     * @param start 开始；类型为 <code>long</code>
     * @param length length；类型为 <code>long</code>
     * @return 类型为 <code>InputStream</code> 的处理结果
     */
    InputStream openRange(String storageKey, long start, long length);

    /**
     * 判断是否存在媒体存储。
     *
     * @param storageKey 媒体在存储根目录内的相对键；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    boolean exists(String storageKey);

    /**
     * 大小处理入口。
     *
     * @param storageKey 媒体在存储根目录内的相对键；类型为 <code>String</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
    long size(String storageKey);

    /**
     * 删除媒体存储。
     *
     * @param storageKey 媒体在存储根目录内的相对键；类型为 <code>String</code>
     */
    void delete(String storageKey);

    // 对象最后写入时间；对象不存在时返回 null。供孤儿文件宽限期判断使用
    /**
     * 最近Modified处理入口。
     *
     * @param storageKey 媒体在存储根目录内的相对键；类型为 <code>String</code>
     * @return 类型为 <code>Instant</code> 的处理结果
     */
    Instant lastModified(String storageKey);

    // 列出全部 storageKey，供孤儿文件清理使用
    /**
     * 列出Keys。
     * @return 本次处理得到的条目集合
     */
    List<String> listKeys();
}
