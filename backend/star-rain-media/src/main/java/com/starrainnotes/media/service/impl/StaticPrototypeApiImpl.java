/*
 * StaticPrototypeApiImpl：媒体模块的业务实现文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：StaticPrototypeApi、MediaAssetEntity、MediaAssetNotFoundException、MediaPrototypeInvalidException、MediaAssetMapper、MediaStorage。
 * 阅读入口：validate（校验StaticPrototype）；read（读取StaticPrototype）；contentType（内容类型或请求声明的 Content-Type处理入口）。
 */

package com.starrainnotes.media.service.impl;

import com.starrainnotes.media.api.StaticPrototypeApi;
import com.starrainnotes.media.entity.MediaAssetEntity;
import com.starrainnotes.media.exception.MediaAssetNotFoundException;
import com.starrainnotes.media.exception.MediaPrototypeInvalidException;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.storage.MediaStorage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * StaticPrototype接口实现，属于媒体模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaticPrototypeApiImpl implements StaticPrototypeApi {
    /**
     * ARCHIVE。
     */
    private static final long MAX_ARCHIVE = 25L * 1024 * 1024;
    /**
     * EXPANDED。
     */
    private static final long MAX_EXPANDED = 100L * 1024 * 1024;
    /**
     * 文件。
     */
    private static final int MAX_FILE = 10 * 1024 * 1024;
    /**
     * TYPES。
     */
    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("html", "text/html"), Map.entry("css", "text/css"), Map.entry("js", "text/javascript"),
            Map.entry("json", "application/json"), Map.entry("map", "application/json"),
            Map.entry("svg", "image/svg+xml"), Map.entry("png", "image/png"), Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"), Map.entry("webp", "image/webp"), Map.entry("gif", "image/gif"),
            Map.entry("ico", "image/x-icon"), Map.entry("woff", "font/woff"), Map.entry("woff2", "font/woff2"),
            Map.entry("ttf", "font/ttf"), Map.entry("txt", "text/plain"));
    /**
     * assets。
     * 协作类型：<code>MediaAssetMapper</code>。
     */
    private final MediaAssetMapper assets;
    /**
     * 存储。
     */
    private final MediaStorage storage;

    /**
     * 校验StaticPrototype。
     * 关键处理步骤调用 <code>entryPath.endsWith</code>、<code>invalid</code>、<code>read</code>。
     *
     * @param assetId 资产标识；类型为 <code>Long</code>
     * @param entryPath 入口路径；类型为 <code>String</code>
     */
    @Override public void validate(Long assetId, String entryPath) {
        if (entryPath == null || !entryPath.endsWith(".html")) { invalid("原型入口需要 HTML 文件"); }
        read(assetId, entryPath);
    }
    /**
     * 读取StaticPrototype。
     * 主要协作调用：<code>assets.assetById</code>、<code>storage.open</code>。
     * 实现中的显式失败类型包括 <code>MediaAssetNotFoundException</code>、<code>MediaPrototypeInvalidException</code>；具体触发条件见对应分支。
     *
     * @param assetId 资产标识；类型为 <code>Long</code>
     * @param path 路径；类型为 <code>String</code>
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    @Override public byte[] read(Long assetId, String path) {
        safePath(path);
        MediaAssetEntity asset = assets.assetById(assetId);
        if (asset == null) { throw new MediaAssetNotFoundException(); }
        if (!"ACTIVE".equals(asset.getStatus()) || !"PUBLIC".equals(asset.getAccessLevel())
                || !"zip".equalsIgnoreCase(asset.getFileExtension()) || asset.getSizeBytes() > MAX_ARCHIVE) {
            invalid("原型必须使用不超过 25 MB 的公开 ZIP 媒体");
        }
        byte[] found = null;
        Set<String> names = new HashSet<>();
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(storage.open(asset.getStorageKey()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (names.size() >= 2000 || !names.add(name)) { invalid("原型文件过多或包含重复路径"); }
                if (entry.isDirectory()) { safePath(name.substring(0, name.length() - 1)); zip.closeEntry(); continue; }
                safePath(name);
                contentType(name);
                boolean wanted = path.equals(name);
                ByteArrayOutputStream bytes = wanted ? new ByteArrayOutputStream() : null;
                int size = 0, read;
                byte[] buffer = new byte[8192];
                while ((read = zip.read(buffer)) != -1) {
                    size += read; total += read;
                    if (size > MAX_FILE || total > MAX_EXPANDED) { invalid("原型解压大小超出限制"); }
                    if (wanted) { bytes.write(buffer, 0, read); }
                }
                if (wanted) { found = bytes.toByteArray(); }
                zip.closeEntry();
            }
        } catch (IOException exception) {
            throw new MediaPrototypeInvalidException("无法读取原型 ZIP 文件");
        }
        if (found == null) { invalid("原型文件不存在"); }
        return found;
    }
    /**
     * 内容类型或请求声明的 Content-Type处理入口。
     * 主要协作调用：<code>TYPES.get</code>。
     *
     * @param path 路径；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override public String contentType(String path) {
        safePath(path);
        int dot = path.lastIndexOf('.');
        String type = dot < 0 ? null : TYPES.get(path.substring(dot + 1).toLowerCase(Locale.ROOT));
        if (type == null) { invalid("原型包含不支持的文件类型"); }
        return type;
    }
    /**
     * 安全路径处理入口。
     * 关键处理步骤调用 <code>path.isBlank</code>、<code>path.length</code>、<code>path.startsWith</code>、<code>path.contains</code>。
     *
     * @param path 路径；类型为 <code>String</code>
     */
    private void safePath(String path) {
        if (path == null || path.isBlank() || path.length() > 500 || path.startsWith("/")
                || path.contains("\\") || path.contains(":") || path.contains("%") || path.chars().anyMatch(c -> c < 32)) {
            invalid("原型路径无效");
        }
        for (String part : path.split("/", -1)) {
            if (part.isEmpty() || ".".equals(part) || "..".equals(part)) { invalid("原型路径不能跨越目录"); }
        }
    }
    /**
     * 无效处理入口。
     * 实现中的显式失败类型包括 <code>MediaPrototypeInvalidException</code>；具体触发条件见对应分支。
     *
     * @param message 留言；类型为 <code>String</code>
     */
    private void invalid(String message) { throw new MediaPrototypeInvalidException(message); }
}
