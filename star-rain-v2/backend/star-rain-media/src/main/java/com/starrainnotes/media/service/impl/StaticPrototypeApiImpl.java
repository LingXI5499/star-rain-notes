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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaticPrototypeApiImpl implements StaticPrototypeApi {
    private static final long MAX_ARCHIVE = 25L * 1024 * 1024;
    private static final long MAX_EXPANDED = 100L * 1024 * 1024;
    private static final int MAX_FILE = 10 * 1024 * 1024;
    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("html", "text/html"), Map.entry("css", "text/css"), Map.entry("js", "text/javascript"),
            Map.entry("json", "application/json"), Map.entry("map", "application/json"),
            Map.entry("svg", "image/svg+xml"), Map.entry("png", "image/png"), Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"), Map.entry("webp", "image/webp"), Map.entry("gif", "image/gif"),
            Map.entry("ico", "image/x-icon"), Map.entry("woff", "font/woff"), Map.entry("woff2", "font/woff2"),
            Map.entry("ttf", "font/ttf"), Map.entry("txt", "text/plain"));
    private final MediaAssetMapper assets;
    private final MediaStorage storage;

    @Override public void validate(Long assetId, String entryPath) {
        if (entryPath == null || !entryPath.endsWith(".html")) { invalid("原型入口需要 HTML 文件"); }
        read(assetId, entryPath);
    }
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
    @Override public String contentType(String path) {
        safePath(path);
        int dot = path.lastIndexOf('.');
        String type = dot < 0 ? null : TYPES.get(path.substring(dot + 1).toLowerCase(Locale.ROOT));
        if (type == null) { invalid("原型包含不支持的文件类型"); }
        return type;
    }
    private void safePath(String path) {
        if (path == null || path.isBlank() || path.length() > 500 || path.startsWith("/")
                || path.contains("\\") || path.contains(":") || path.contains("%") || path.chars().anyMatch(c -> c < 32)) {
            invalid("原型路径无效");
        }
        for (String part : path.split("/", -1)) {
            if (part.isEmpty() || ".".equals(part) || "..".equals(part)) { invalid("原型路径不能跨越目录"); }
        }
    }
    private void invalid(String message) { throw new MediaPrototypeInvalidException(message); }
}
