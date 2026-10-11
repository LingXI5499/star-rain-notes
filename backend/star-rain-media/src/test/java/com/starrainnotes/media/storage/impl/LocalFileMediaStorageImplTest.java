/*
 * LocalFileMediaStorageImplTest：媒体模块的测试文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：ApiException、MediaProperties、MediaStorageResultDTO。
 * 阅读入口：setUp（初始化当前测试场景的被测对象与模拟依赖）；writeFailureDoesNotExposePath（\u5199\u5165\u76ee\u5f55\u4e0d\u53ef\u7528\u65f6\u4fdd\u7559\u6839\u56e0\uff0c\u4f46\u54cd\u5e94\u4e0d\u6cc4\u9732\u78c1\u76d8\u8def\u5f84）；keyLayout（storageKey \u5f62\u5982 YYYY/MM/<uuid>.<ext>\uff0c\u4e14\u4e0d\u542b\u7edd\u5bf9\u8def\u5f84）；doesNotOverwrite（\u76f8\u540c\u5185\u5bb9\u91cd\u590d\u5199\u5165\u5f97\u5230\u4e0d\u540c Key\uff0c\u7edd\u4e0d\u8986\u76d6）。
 */

package com.starrainnotes.media.storage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.properties.MediaProperties;
import com.starrainnotes.media.dto.MediaStorageResultDTO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/*
 * 本地存储单元测试。
 * 重点：Key 布局与唯一性、同名不覆盖、区间读取、路径越界一律按“对象缺失”处理。
 */
/**
 * Local文件媒体存储实现测试，属于媒体模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class LocalFileMediaStorageImplTest {

    /**
     * tempDir。
     */
    @TempDir
    Path tempDir;

    /**
     * 存储。
     */
    private LocalFileMediaStorageImpl storage;

    /**
     * 初始化当前测试场景的被测对象与模拟依赖。
     * 主要协作调用：<code>tempDir.toString</code>。
     */
    @BeforeEach
    void setUp() {
        MediaProperties properties = new MediaProperties();
        properties.setStorageRoot(tempDir.toString());
        storage = new LocalFileMediaStorageImpl(properties);
    }

    /**
     * \u5199\u5165\u76ee\u5f55\u4e0d\u53ef\u7528\u65f6\u4fdd\u7559\u6839\u56e0\uff0c\u4f46\u54cd\u5e94\u4e0d\u6cc4\u9732\u78c1\u76d8\u8def\u5f84。
     * 主要协作调用：<code>tempDir.resolve</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @DisplayName("写入目录不可用时保留根因，但响应不泄露磁盘路径")
    void writeFailureDoesNotExposePath() throws IOException {
        Path blocked = tempDir.resolve("private-location");
        Files.writeString(blocked, "not a directory");
        MediaProperties properties = new MediaProperties();
        properties.setStorageRoot(blocked.toString());
        LocalFileMediaStorageImpl unavailable = new LocalFileMediaStorageImpl(properties);

        assertThatThrownBy(() -> unavailable.store(content("hello"), "png"))
                .isInstanceOf(ApiException.class)
                .hasCauseInstanceOf(IOException.class)
                .hasMessageNotContaining(blocked.toString())
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_STORAGE_WRITE_FAILED");
    }

    /**
     * storageKey \u5f62\u5982 YYYY/MM/&lt;uuid&gt;.&lt;ext&gt;\uff0c\u4e14\u4e0d\u542b\u7edd\u5bf9\u8def\u5f84。
     * 主要协作调用：<code>storage.store</code>、<code>tempDir.toString</code>、<code>storage.exists</code>、<code>storage.size</code>。
     */
    @Test
    @DisplayName("storageKey 形如 YYYY/MM/<uuid>.<ext>，且不含绝对路径")
    void keyLayout() {
        MediaStorageResultDTO stored = storage.store(content("hello"), "png");

        assertThat(stored.getStorageKey()).matches("\\d{4}/\\d{2}/[0-9a-f-]{36}\\.png");
        assertThat(stored.getStorageProvider()).isEqualTo("LOCAL");
        assertThat(stored.getSizeBytes()).isEqualTo(5);
        assertThat(stored.getStorageKey()).doesNotContain(tempDir.toString());
        assertThat(storage.exists(stored.getStorageKey())).isTrue();
        assertThat(storage.size(stored.getStorageKey())).isEqualTo(5);
    }

    /**
     * \u76f8\u540c\u5185\u5bb9\u91cd\u590d\u5199\u5165\u5f97\u5230\u4e0d\u540c Key\uff0c\u7edd\u4e0d\u8986\u76d6。
     * 主要协作调用：<code>storage.store</code>、<code>storage.exists</code>。
     */
    @Test
    @DisplayName("相同内容重复写入得到不同 Key，绝不覆盖")
    void doesNotOverwrite() {
        MediaStorageResultDTO first = storage.store(content("same-bytes"), "png");
        MediaStorageResultDTO second = storage.store(content("same-bytes"), "png");

        assertThat(first.getStorageKey()).isNotEqualTo(second.getStorageKey());
        assertThat(storage.exists(first.getStorageKey())).isTrue();
        assertThat(storage.exists(second.getStorageKey())).isTrue();
    }

    /**
     * open \u8bfb\u56de\u539f\u59cb\u5b57\u8282。
     * 主要协作调用：<code>storage.store</code>、<code>storage.open</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @DisplayName("open 读回原始字节")
    void openReturnsContent() throws IOException {
        MediaStorageResultDTO stored = storage.store(content("内容内容"), "txt");

        try (var in = storage.open(stored.getStorageKey())) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("内容内容");
        }
    }

    /**
     * openRange \u53ea\u8fd4\u56de\u8bf7\u6c42\u7684\u533a\u95f4\uff1a\u97f3\u9891/\u89c6\u9891\u62d6\u52a8\u8fdb\u5ea6\u4f9d\u8d56\u5b83。
     * 主要协作调用：<code>storage.store</code>、<code>storage.openRange</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @DisplayName("openRange 只返回请求的区间：音频/视频拖动进度依赖它")
    void openRangeReturnsSlice() throws IOException {
        byte[] payload = "0123456789".getBytes(StandardCharsets.UTF_8);
        MediaStorageResultDTO stored = storage.store(new ByteArrayInputStream(payload), "txt");

        try (var in = storage.openRange(stored.getStorageKey(), 3, 4)) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("3456");
        }
        // 越界的 length 由底层文件长度截断，不会抛异常
        try (var in = storage.openRange(stored.getStorageKey(), 8, 100)) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("89");
        }
    }

    /**
     * \u5bf9\u8c61\u4e0d\u5b58\u5728\u65f6\u62a5 MEDIA_CONTENT_MISSING。
     * 主要协作调用：<code>storage.open</code>、<code>storage.size</code>。
     */
    @Test
    @DisplayName("对象不存在时报 MEDIA_CONTENT_MISSING")
    void openMissing() {
        assertThatThrownBy(() -> storage.open("2026/10/does-not-exist.png"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_CONTENT_MISSING");
        assertThat(storage.size("2026/10/missing.png")).isNegative();
    }

    /**
     * \u8d8a\u754c storageKey \u4e0d\u8bfb\u6587\u4ef6\u3001\u4e0d\u6cc4\u6f0f\u670d\u52a1\u5668\u8def\u5f84。
     * 主要协作调用：<code>storage.open</code>、<code>storage.exists</code>。
     */
    @Test
    @DisplayName("越界 storageKey 不读文件、不泄漏服务器路径")
    void rejectsPathTraversal() {
        for (String key : List.of("../../etc/passwd", "..\\..\\windows\\win.ini",
                "/etc/passwd", "C:/Windows/win.ini", "")) {
            assertThatThrownBy(() -> storage.open(key))
                    .as("key=%s", key)
                    .isInstanceOf(ApiException.class)
                    .extracting(ex -> ((ApiException) ex).getCode())
                    .isEqualTo("MEDIA_CONTENT_MISSING");
            assertThat(storage.exists(key)).isFalse();
        }
    }

    /**
     * delete \u5e42\u7b49\uff1a\u5220\u4e0d\u5b58\u5728\u7684 Key \u4e0d\u62a5\u9519。
     * 主要协作调用：<code>storage.store</code>、<code>storage.delete</code>、<code>storage.exists</code>。
     */
    @Test
    @DisplayName("delete 幂等：删不存在的 Key 不报错")
    void deleteIsIdempotent() {
        MediaStorageResultDTO stored = storage.store(content("bye"), "png");

        storage.delete(stored.getStorageKey());
        assertThat(storage.exists(stored.getStorageKey())).isFalse();
        storage.delete(stored.getStorageKey());
    }

    /**
     * lastModified \u7528\u4e8e\u5b64\u513f\u6587\u4ef6\u5bbd\u9650\u671f\u5224\u65ad。
     * 主要协作调用：<code>storage.store</code>、<code>storage.lastModified</code>。
     */
    @Test
    @DisplayName("lastModified 用于孤儿文件宽限期判断")
    void lastModifiedForGracePeriod() {
        MediaStorageResultDTO stored = storage.store(content("aged"), "png");

        assertThat(storage.lastModified(stored.getStorageKey())).isNotNull();
        assertThat(storage.lastModified("2026/10/missing.png")).isNull();
    }

    /**
     * listKeys \u8fd4\u56de\u76f8\u5bf9 Key\uff0c\u4f9b\u5b64\u513f\u6e05\u7406\u5bf9\u6bd4\u6570\u636e\u5e93。
     * 主要协作调用：<code>storage.store</code>、<code>storage.listKeys</code>。
     */
    @Test
    @DisplayName("listKeys 返回相对 Key，供孤儿清理对比数据库")
    void listKeysUsesRelativeSlashSeparatedKeys() {
        MediaStorageResultDTO stored = storage.store(content("listed"), "png");

        assertThat(storage.listKeys()).containsExactly(stored.getStorageKey());
        assertThat(storage.listKeys().get(0)).doesNotContain("\\");
    }

    /**
     * \u5b58\u50a8\u6839\u76ee\u5f55\u4e0d\u5b58\u5728\u65f6 listKeys \u8fd4\u56de\u7a7a\u5217\u8868\u800c\u4e0d\u662f\u629b\u5f02\u5e38。
     * 主要协作调用：<code>tempDir.resolve</code>。
     */
    @Test
    @DisplayName("存储根目录不存在时 listKeys 返回空列表而不是抛异常")
    void listKeysOnMissingRoot() {
        MediaProperties properties = new MediaProperties();
        properties.setStorageRoot(tempDir.resolve("not-created").toString());

        assertThat(new LocalFileMediaStorageImpl(properties).listKeys()).isEmpty();
    }

    /**
     * \u6269\u5c55\u540d\u4e2d\u7684\u8def\u5f84\u5b57\u7b26\u88ab\u6e05\u7406\uff0c\u4e0d\u4f1a\u6784\u9020\u51fa\u8d8a\u754c\u76ee\u5f55。
     * 主要协作调用：<code>storage.store</code>、<code>tempDir.resolve</code>。
     */
    @Test
    @DisplayName("扩展名中的路径字符被清理，不会构造出越界目录")
    void normalizesExtension() {
        MediaStorageResultDTO stored = storage.store(content("x"), "../evil");

        assertThat(stored.getStorageKey()).matches("\\d{4}/\\d{2}/[0-9a-f-]{36}\\.evil");
        assertThat(Files.exists(tempDir.resolve(stored.getStorageKey()))).isTrue();
    }

    /**
     * 内容处理入口。
     * 关键处理步骤调用 <code>value.getBytes</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>ByteArrayInputStream</code> 的处理结果
     */
    private static ByteArrayInputStream content(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
