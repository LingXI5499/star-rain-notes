package com.starrainnotes.media.storage.impl;

import com.starrainnotes.media.exception.MediaContentMissingException;
import com.starrainnotes.media.exception.MediaStorageDeleteException;
import com.starrainnotes.media.exception.MediaStorageWriteException;
import com.starrainnotes.media.properties.MediaProperties;
import com.starrainnotes.media.storage.MediaStorage;
import com.starrainnotes.media.storage.StorageWriteCommand;
import com.starrainnotes.media.storage.StoredObject;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/*
 * 本地文件系统存储实现。
 *
 * 目录布局：<storageRoot>/YYYY/MM/<uuid>.<ext>
 * 数据库只保存相对 Key，绝对根路径只存在于配置中。
 *
 * 写入用 CREATE_NEW：同名文件不会被覆盖，UUID 撞了就换 Key 重试。
 * 内容以流式复制落盘，因此 200MB 视频不会占用等量堆内存。
 */
@Component
public class LocalFileMediaStorage implements MediaStorage {

    private static final DateTimeFormatter MONTH_PATH = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final int MAX_KEY_ATTEMPTS = 3;

    private final Path root;
    private final String provider;

    public LocalFileMediaStorage(MediaProperties properties) {
        this.root = Path.of(properties.getStorageRoot()).toAbsolutePath().normalize();
        this.provider = properties.getStorageProvider();
    }

    @Override
    public String provider() {
        return provider;
    }

    @Override
    public StoredObject store(StorageWriteCommand command) {
        String extension = normalizeExtension(command.getFileExtension());
        for (int attempt = 0; attempt < MAX_KEY_ATTEMPTS; attempt++) {
            String key = keyFor(extension);
            Path target = root.resolve(key).normalize();
            try {
                Files.createDirectories(target.getParent());
                /*
                 * 手动流转写而不是 Files.copy：copy 无法表达 CREATE_NEW，
                 * 而“同名文件绝不覆盖”是硬要求。transferTo 同时返回写入字节数。
                 */
                long written;
                try (OutputStream out = Files.newOutputStream(target,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                    written = command.getContent().transferTo(out);
                }
                return new StoredObject(provider, key, written);
            } catch (FileAlreadyExistsException ex) {
                // 同名文件不覆盖，换一个 Key 重试
            } catch (IOException ex) {
                throw new MediaStorageWriteException(ex);
            }
        }
        throw new MediaStorageWriteException(new IOException("无法生成唯一 storageKey"));
    }

    @Override
    public InputStream open(String storageKey) {
        Path target = resolveWithinRoot(storageKey);
        if (!Files.isRegularFile(target)) {
            // 数据库有记录但文件不在：存储与数据库不一致，需要运维介入
            throw new MediaContentMissingException();
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException ex) {
            throw new MediaContentMissingException();
        }
    }

    @Override
    public InputStream openRange(String storageKey, long start, long length) {
        Path target = resolveWithinRoot(storageKey);
        if (!Files.isRegularFile(target)) {
            throw new MediaContentMissingException();
        }
        try {
            SeekableByteChannel channel = Files.newByteChannel(target, StandardOpenOption.READ);
            // 定位到区间起点后再交给上层；close 会同时关闭底层 channel
            channel.position(start);
            return new BoundedInputStream(Channels.newInputStream(channel), length);
        } catch (IOException ex) {
            throw new MediaContentMissingException();
        }
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            return Files.isRegularFile(resolveWithinRoot(storageKey));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @Override
    public long size(String storageKey) {
        try {
            Path target = resolveWithinRoot(storageKey);
            return Files.isRegularFile(target) ? Files.size(target) : -1L;
        } catch (IOException | RuntimeException ex) {
            return -1L;
        }
    }

    @Override
    public void delete(String storageKey) {
        Path target = resolveWithinRoot(storageKey);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new MediaStorageDeleteException(ex);
        }
    }

    @Override
    public Instant lastModified(String storageKey) {
        try {
            Path target = resolveWithinRoot(storageKey);
            if (!Files.isRegularFile(target)) {
                return null;
            }
            return Files.getLastModifiedTime(target).toInstant();
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    @Override
    public List<String> listKeys() {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<String> keys = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> keys.add(root.relativize(path).toString().replace('\\', '/')));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        keys.sort(String::compareTo);
        return keys;
    }

    private String keyFor(String extension) {
        String month = LocalDate.now(ZoneOffset.UTC).format(MONTH_PATH);
        String name = UUID.randomUUID().toString();
        return extension.isEmpty() ? month + "/" + name : month + "/" + name + "." + extension;
    }

    /*
     * 把外部传入的 storageKey 解析到根目录内。
     * 任何越界 Key（../、绝对路径、空值）都视为对象缺失，
     * 既不读文件也不向调用方泄漏服务器路径。
     */
    private Path resolveWithinRoot(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new MediaContentMissingException();
        }
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            throw new MediaContentMissingException();
        }
        return target;
    }

    private static String normalizeExtension(String extension) {
        if (extension == null) {
            return "";
        }
        String trimmed = extension.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith(".")) {
            trimmed = trimmed.substring(1);
        }
        // 只保留字母数字，避免扩展名参与路径构造
        return trimmed.replaceAll("[^a-z0-9]", "");
    }

    /*
     * 限定最多读取 length 个字节的包装流。
     * Range 响应的 Content-Length 已由 Controller 显式设置，
     * 这里只是保证实际写出的字节数不会超出请求区间。
     */
    private static final class BoundedInputStream extends FilterInputStream {

        private long remaining;

        private BoundedInputStream(InputStream delegate, long limit) {
            super(delegate);
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int value = super.read();
            if (value >= 0) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int allowed = (int) Math.min(length, remaining);
            int read = super.read(buffer, offset, allowed);
            if (read > 0) {
                remaining -= read;
            }
            return read;
        }
    }
}
