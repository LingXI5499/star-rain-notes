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
class LocalFileMediaStorageImplTest {

    @TempDir
    Path tempDir;

    private LocalFileMediaStorageImpl storage;

    @BeforeEach
    void setUp() {
        MediaProperties properties = new MediaProperties();
        properties.setStorageRoot(tempDir.toString());
        storage = new LocalFileMediaStorageImpl(properties);
    }

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

    @Test
    @DisplayName("相同内容重复写入得到不同 Key，绝不覆盖")
    void doesNotOverwrite() {
        MediaStorageResultDTO first = storage.store(content("same-bytes"), "png");
        MediaStorageResultDTO second = storage.store(content("same-bytes"), "png");

        assertThat(first.getStorageKey()).isNotEqualTo(second.getStorageKey());
        assertThat(storage.exists(first.getStorageKey())).isTrue();
        assertThat(storage.exists(second.getStorageKey())).isTrue();
    }

    @Test
    @DisplayName("open 读回原始字节")
    void openReturnsContent() throws IOException {
        MediaStorageResultDTO stored = storage.store(content("内容内容"), "txt");

        try (var in = storage.open(stored.getStorageKey())) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("内容内容");
        }
    }

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

    @Test
    @DisplayName("对象不存在时报 MEDIA_CONTENT_MISSING")
    void openMissing() {
        assertThatThrownBy(() -> storage.open("2026/10/does-not-exist.png"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_CONTENT_MISSING");
        assertThat(storage.size("2026/10/missing.png")).isNegative();
    }

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

    @Test
    @DisplayName("delete 幂等：删不存在的 Key 不报错")
    void deleteIsIdempotent() {
        MediaStorageResultDTO stored = storage.store(content("bye"), "png");

        storage.delete(stored.getStorageKey());
        assertThat(storage.exists(stored.getStorageKey())).isFalse();
        storage.delete(stored.getStorageKey());
    }

    @Test
    @DisplayName("lastModified 用于孤儿文件宽限期判断")
    void lastModifiedForGracePeriod() {
        MediaStorageResultDTO stored = storage.store(content("aged"), "png");

        assertThat(storage.lastModified(stored.getStorageKey())).isNotNull();
        assertThat(storage.lastModified("2026/10/missing.png")).isNull();
    }

    @Test
    @DisplayName("listKeys 返回相对 Key，供孤儿清理对比数据库")
    void listKeysUsesRelativeSlashSeparatedKeys() {
        MediaStorageResultDTO stored = storage.store(content("listed"), "png");

        assertThat(storage.listKeys()).containsExactly(stored.getStorageKey());
        assertThat(storage.listKeys().get(0)).doesNotContain("\\");
    }

    @Test
    @DisplayName("存储根目录不存在时 listKeys 返回空列表而不是抛异常")
    void listKeysOnMissingRoot() {
        MediaProperties properties = new MediaProperties();
        properties.setStorageRoot(tempDir.resolve("not-created").toString());

        assertThat(new LocalFileMediaStorageImpl(properties).listKeys()).isEmpty();
    }

    @Test
    @DisplayName("扩展名中的路径字符被清理，不会构造出越界目录")
    void normalizesExtension() {
        MediaStorageResultDTO stored = storage.store(content("x"), "../evil");

        assertThat(stored.getStorageKey()).matches("\\d{4}/\\d{2}/[0-9a-f-]{36}\\.evil");
        assertThat(Files.exists(tempDir.resolve(stored.getStorageKey()))).isTrue();
    }

    private static ByteArrayInputStream content(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
