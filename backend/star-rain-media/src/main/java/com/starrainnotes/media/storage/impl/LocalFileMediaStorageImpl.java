/*
 * LocalFileMediaStorageImpl：媒体模块的存储适配文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 实现文件或存储访问边界，向业务服务提供读写能力，并将底层失败转换为本模块约定的异常。
 * 主要协作类型：MediaContentMissingException、MediaStorageDeleteException、MediaStorageWriteException、MediaProperties、MediaStorage、MediaStorageResultDTO。
 * 阅读入口：provider（提供方处理入口）；store（存储Local文件媒体存储实现）；open（打开Local文件媒体存储实现）；openRange（打开范围）。
 */

package com.starrainnotes.media.storage.impl;

import com.starrainnotes.media.exception.MediaContentMissingException;
import com.starrainnotes.media.exception.MediaStorageDeleteException;
import com.starrainnotes.media.exception.MediaStorageWriteException;
import com.starrainnotes.media.properties.MediaProperties;
import com.starrainnotes.media.storage.MediaStorage;
import com.starrainnotes.media.dto.MediaStorageResultDTO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
/**
 * Local文件媒体存储实现，属于媒体模块。
 * 实现文件或存储访问边界，向业务服务提供读写能力，并将底层失败转换为本模块约定的异常。
 */
@Component
public class LocalFileMediaStorageImpl implements MediaStorage {

    /**
     * 当前类的日志记录器；日志参数不能包含密码或会话令牌。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileMediaStorageImpl.class);

    /**
     * 路径。
     */
    private static final DateTimeFormatter MONTH_PATH = DateTimeFormatter.ofPattern("yyyy/MM");
    /**
     * ATTEMPTS。
     * 初始值为 <code>3</code>。
     */
    private static final int MAX_KEY_ATTEMPTS = 3;

    /**
     * root。
     */
    private final Path root;
    /**
     * 提供方。
     */
    private final String provider;

    /**
     * 构造LocalFileMediaStorageImpl，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>Path.of</code>、<code>properties.getStorageRoot</code>、<code>properties.getStorageProvider</code>、<code>Files.exists</code>。
     *
     * @param properties 本模块的外部配置属性；类型为 <code>MediaProperties</code>
     */
    public LocalFileMediaStorageImpl(MediaProperties properties) {
        this.root = Path.of(properties.getStorageRoot()).toAbsolutePath().normalize();
        this.provider = properties.getStorageProvider();
        LOGGER.info("Media storage initialized; provider={}, root={}, exists={}, directory={}, writable={}",
                provider, root, Files.exists(root), Files.isDirectory(root), Files.isWritable(root));
    }

    /**
     * 提供方处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    public String provider() {
        return provider;
    }

    /**
     * 存储Local文件媒体存储实现。
     * 主要协作调用：<code>root.resolve</code>。
     * 实现中的显式失败类型包括 <code>MediaStorageWriteException</code>；具体触发条件见对应分支。
     *
     * @param content 内容；类型为 <code>InputStream</code>
     * @param fileExtension 文件扩展名；类型为 <code>String</code>
     * @return 类型为 <code>MediaStorageResultDTO</code> 的处理结果
     */
    @Override
    public MediaStorageResultDTO store(InputStream content, String fileExtension) {
        String extension = normalizeExtension(fileExtension);
        for (int attempt = 0; attempt < MAX_KEY_ATTEMPTS; attempt++) {
            String key = keyFor(extension);
            Path target = root.resolve(key).normalize();
            boolean created = false;
            try {
                Files.createDirectories(target.getParent());
                /*
                 * 手动流转写而不是 Files.copy：copy 无法表达 CREATE_NEW，
                 * 而“同名文件绝不覆盖”是硬要求。transferTo 同时返回写入字节数。
                 */
                long written;
                try (OutputStream out = Files.newOutputStream(target,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                    created = true;
                    written = content.transferTo(out);
                }
                return new MediaStorageResultDTO(provider, key, written);
            } catch (FileAlreadyExistsException ex) {
                // 同名文件不覆盖，换一个 Key 重试
            } catch (IOException ex) {
                if (created) {
                    try { Files.deleteIfExists(target); }
                    catch (IOException cleanupFailure) { ex.addSuppressed(cleanupFailure); }
                }
                LOGGER.error("Media storage write failed; root={}, key={}", root, key, ex);
                throw new MediaStorageWriteException(ex);
            }
        }
        throw new MediaStorageWriteException(new IOException("无法生成唯一 storageKey"));
    }

    /**
     * 打开Local文件媒体存储实现。
     * 关键处理步骤调用 <code>resolveWithinRoot</code>、<code>Files.isRegularFile</code>、<code>Files.newInputStream</code>。
     * 实现中的显式失败类型包括 <code>MediaContentMissingException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(!Files.isRegularFile(target))</code>。
     *
     * @param storageKey 媒体在存储根目录内的相对键；类型为 <code>String</code>
     * @return 类型为 <code>InputStream</code> 的处理结果
     */
    @Override
    public InputStream open(String storageKey) {
        Path target = resolveWithinRoot(storageKey);
        if (!Files.isRegularFile(target)) {
            // 数据库有记录但文件不在：存储与数据库不一致，需要运维介入
            LOGGER.warn("Media content missing or not a regular file; root={}, key={}", root, storageKey);
            throw new MediaContentMissingException();
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException ex) {
            LOGGER.warn("Media content read failed; root={}, key={}", root, storageKey, ex);
            throw new MediaContentMissingException();
        }
    }

    /**
     * 打开范围。
     * 关键处理步骤调用 <code>resolveWithinRoot</code>、<code>Files.isRegularFile</code>、<code>Files.newByteChannel</code>、<code>channel.position</code>。
     * 实现中的显式失败类型包括 <code>MediaContentMissingException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(!Files.isRegularFile(target))</code>。
     *
     * @param storageKey 媒体在存储根目录内的相对键；类型为 <code>String</code>
     * @param start 开始；类型为 <code>long</code>
     * @param length length；类型为 <code>long</code>
     * @return 类型为 <code>InputStream</code> 的处理结果
     */
    @Override
    public InputStream openRange(String storageKey, long start, long length) {
        Path target = resolveWithinRoot(storageKey);
        if (!Files.isRegularFile(target)) {
            LOGGER.warn("Media content missing or not a regular file; root={}, key={}", root, storageKey);
            throw new MediaContentMissingException();
        }
        try {
            SeekableByteChannel channel = Files.newByteChannel(target, StandardOpenOption.READ);
            // 定位到区间起点后再交给上层；close 会同时关闭底层 channel
            channel.position(start);
            return new BoundedInputStream(Channels.newInputStream(channel), length);
        } catch (IOException ex) {
            LOGGER.warn("Media content range read failed; root={}, key={}", root, storageKey, ex);
            throw new MediaContentMissingException();
        }
    }

    /**
     * 判断是否存在Local文件媒体存储实现。
     * 关键处理步骤调用 <code>Files.isRegularFile</code>、<code>resolveWithinRoot</code>。
     *
     * @param storageKey 媒体在存储根目录内的相对键；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean exists(String storageKey) {
        try {
            return Files.isRegularFile(resolveWithinRoot(storageKey));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * 大小处理入口。
     * 关键处理步骤调用 <code>resolveWithinRoot</code>、<code>Files.isRegularFile</code>、<code>Files.size</code>。
     *
     * @param storageKey 媒体在存储根目录内的相对键；类型为 <code>String</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
    @Override
    public long size(String storageKey) {
        try {
            Path target = resolveWithinRoot(storageKey);
            return Files.isRegularFile(target) ? Files.size(target) : -1L;
        } catch (IOException | RuntimeException ex) {
            return -1L;
        }
    }

    /**
     * 删除Local文件媒体存储实现。
     * 关键处理步骤调用 <code>resolveWithinRoot</code>、<code>Files.deleteIfExists</code>。
     * 实现中的显式失败类型包括 <code>MediaStorageDeleteException</code>；具体触发条件见对应分支。
     *
     * @param storageKey 媒体在存储根目录内的相对键；类型为 <code>String</code>
     */
    @Override
    public void delete(String storageKey) {
        Path target = resolveWithinRoot(storageKey);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new MediaStorageDeleteException(ex);
        }
    }

    /**
     * 最近Modified处理入口。
     * 关键处理步骤调用 <code>resolveWithinRoot</code>、<code>Files.isRegularFile</code>、<code>Files.getLastModifiedTime</code>。
     * 边界判断包含 <code>(!Files.isRegularFile(target))</code>。
     *
     * @param storageKey 媒体在存储根目录内的相对键；类型为 <code>String</code>
     * @return 类型为 <code>Instant</code> 的处理结果
     */
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

    /**
     * 列出Keys。
     * 主要协作调用：<code>root.relativize</code>。
     * 实现中的显式失败类型包括 <code>UncheckedIOException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(!Files.isDirectory(root))</code>。
     * @return 本次处理得到的条目集合
     */
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

    /**
     * 键面向处理入口。
     * 关键处理步骤调用 <code>LocalDate.now</code>、<code>UUID.randomUUID</code>、<code>extension.isEmpty</code>。
     *
     * @param extension 扩展名；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
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
    /**
     * 解析WithinRoot。
     * 主要协作调用：<code>root.resolve</code>。
     * 实现中的显式失败类型包括 <code>MediaContentMissingException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(storageKey == null || storageKey.isBlank())</code>；<code>(!target.startsWith(root) || target.equals(root))</code>。
     *
     * @param storageKey 媒体在存储根目录内的相对键；类型为 <code>String</code>
     * @return 类型为 <code>Path</code> 的处理结果
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

    /**
     * 规范化扩展名。
     * 关键处理步骤调用 <code>extension.trim</code>、<code>trimmed.startsWith</code>、<code>trimmed.substring</code>、<code>trimmed.replaceAll</code>。
     * 边界判断包含 <code>(extension == null)</code>；<code>(trimmed.startsWith("."))</code>。
     *
     * @param extension 扩展名；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
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
    /**
     * BoundedInput数据流，属于媒体模块。
     * 实现文件或存储访问边界，向业务服务提供读写能力，并将底层失败转换为本模块约定的异常。
     */
    private static final class BoundedInputStream extends FilterInputStream {

        /**
         * 剩余。
         */
        private long remaining;

        /**
         * 构造BoundedInputStream，接收本类使用的依赖或初始化参数。
         * 关键处理步骤调用 <code>super</code>。
         *
         * @param delegate delegate；类型为 <code>InputStream</code>
         * @param limit 本次查询或处理的数量上限；类型为 <code>long</code>
         */
        private BoundedInputStream(InputStream delegate, long limit) {
            super(delegate);
            this.remaining = limit;
        }

        /**
         * 读取BoundedInput数据流。
         * 关键处理步骤调用 <code>super.read</code>。
         * 边界判断包含 <code>(remaining &lt;= 0)</code>；<code>(value &gt;= 0)</code>。
         * @return 类型为 <code>int</code> 的处理结果
         * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
         */
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

        /**
         * 读取BoundedInput数据流。
         * 关键处理步骤调用 <code>Math.min</code>、<code>super.read</code>。
         * 边界判断包含 <code>(remaining &lt;= 0)</code>；<code>(read &gt; 0)</code>。
         *
         * @param buffer 缓冲区；类型为 <code>byte[]</code>
         * @param offset 查询跳过的记录数；类型为 <code>int</code>
         * @param length length；类型为 <code>int</code>
         * @return 类型为 <code>int</code> 的处理结果
         * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
         */
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
