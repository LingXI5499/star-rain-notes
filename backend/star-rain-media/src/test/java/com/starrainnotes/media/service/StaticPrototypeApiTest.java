/*
 * StaticPrototypeApiTest：媒体模块的测试文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：MediaAssetEntity、MediaPrototypeInvalidException、MediaAssetMapper、MediaStorage、StaticPrototypeApiImpl。
 * 阅读入口：setup（setup处理入口）；validStaticAssetsAreServedWithoutExtractingFilesystemPaths（有效StaticAssetsAreServed不含ExtractingFilesystemPaths处理入口）；traversalAndExecutableEntriesAreRejected（traversal及ExecutableEntriesAre已拒绝处理入口）；privateArchivedOversizedAndMissingEntryArchivesAreRejected（private已归档Oversized及缺失入口ArchivesAre已拒绝处理入口）。
 */

package com.starrainnotes.media.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.starrainnotes.media.entity.MediaAssetEntity;
import com.starrainnotes.media.exception.MediaPrototypeInvalidException;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.storage.MediaStorage;
import com.starrainnotes.media.service.impl.StaticPrototypeApiImpl;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.*;
import org.junit.jupiter.api.*;
/**
 * StaticPrototype接口测试，属于媒体模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class StaticPrototypeApiTest {
    /**
     * assets。
     * 协作类型：<code>MediaAssetMapper</code>。
     */
    private final MediaAssetMapper assets = mock(MediaAssetMapper.class);
    /**
     * 存储。
     */
    private final MediaStorage storage = mock(MediaStorage.class);
    /**
     * 业务服务。
     */
    private final StaticPrototypeApiImpl service = new StaticPrototypeApiImpl(assets, storage);
    /**
     * 资产。
     */
    private MediaAssetEntity asset;
    /**
     * setup处理入口。
     * 主要协作调用：<code>asset.setId</code>、<code>asset.setStatus</code>、<code>asset.setAccessLevel</code>、<code>asset.setFileExtension</code>、<code>asset.setSizeBytes</code>。
     */
    @BeforeEach void setup() {
        asset = new MediaAssetEntity(); asset.setId(1L); asset.setStatus("ACTIVE"); asset.setAccessLevel("PUBLIC");
        asset.setFileExtension("zip"); asset.setSizeBytes(100L); asset.setStorageKey("fixture.zip");
        when(assets.assetById(1L)).thenReturn(asset);
    }
    /**
     * 有效StaticAssetsAreServed不含ExtractingFilesystemPaths处理入口。
     * 主要协作调用：<code>service.validate</code>、<code>service.read</code>、<code>service.contentType</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void validStaticAssetsAreServedWithoutExtractingFilesystemPaths() throws Exception {
        archive(Map.of("index.html", "<h1>Prototype</h1>", "assets/app.js", "document.title='Works'"));
        service.validate(1L,"index.html");
        assertThat(new String(service.read(1L,"assets/app.js"),StandardCharsets.UTF_8)).contains("Works");
        assertThat(service.contentType("assets/app.js")).isEqualTo("text/javascript");
        verify(storage, never()).store(any(),any());
    }
    /**
     * traversal及ExecutableEntriesAre已拒绝处理入口。
     * 主要协作调用：<code>service.validate</code>、<code>service.read</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void traversalAndExecutableEntriesAreRejected() throws Exception {
        archive(Map.of("index.html","safe","../escape.js","bad"));
        assertThatThrownBy(() -> service.validate(1L,"index.html")).isInstanceOf(MediaPrototypeInvalidException.class);
        archive(Map.of("index.html","safe","run.exe","bad"));
        assertThatThrownBy(() -> service.validate(1L,"index.html")).isInstanceOf(MediaPrototypeInvalidException.class);
        assertThatThrownBy(() -> service.read(1L,"..%2fsecret")).isInstanceOf(MediaPrototypeInvalidException.class);
        assertThatThrownBy(() -> service.read(1L,"C:/secret.html")).isInstanceOf(MediaPrototypeInvalidException.class);
    }
    /**
     * private已归档Oversized及缺失入口ArchivesAre已拒绝处理入口。
     * 主要协作调用：<code>asset.setAccessLevel</code>、<code>service.validate</code>、<code>asset.setStatus</code>、<code>asset.setSizeBytes</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void privateArchivedOversizedAndMissingEntryArchivesAreRejected() throws Exception {
        archive(Map.of("index.html","safe"));
        asset.setAccessLevel("PRIVATE"); assertThatThrownBy(() -> service.validate(1L,"index.html")).isInstanceOf(MediaPrototypeInvalidException.class);
        asset.setAccessLevel("PUBLIC"); asset.setStatus("ARCHIVED"); assertThatThrownBy(() -> service.validate(1L,"index.html")).isInstanceOf(MediaPrototypeInvalidException.class);
        asset.setStatus("ACTIVE"); asset.setSizeBytes(26L*1024*1024); assertThatThrownBy(() -> service.validate(1L,"index.html")).isInstanceOf(MediaPrototypeInvalidException.class);
        asset.setSizeBytes(100L); assertThatThrownBy(() -> service.validate(1L,"missing.html")).isInstanceOf(MediaPrototypeInvalidException.class);
    }
    /**
     * expanded文件LimitsAreMeasuredRatherThanTrustingZip响应头处理入口。
     * 主要协作调用：<code>service.validate</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void expandedFileLimitsAreMeasuredRatherThanTrustingZipHeaders() throws Exception {
        archive(Map.of("index.html","a".repeat(10*1024*1024+1)));
        assertThatThrownBy(() -> service.validate(1L,"index.html")).isInstanceOf(MediaPrototypeInvalidException.class);
    }
    /**
     * archive处理入口。
     * 主要协作调用：<code>storage.open</code>。
     *
     * @param entries entries；类型为 <code>Map&lt;String, String&gt;</code>
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    private void archive(Map<String,String> entries) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            for (var item : entries.entrySet()) { zip.putNextEntry(new ZipEntry(item.getKey())); zip.write(item.getValue().getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); }
        }
        when(storage.open("fixture.zip")).thenAnswer(i -> new ByteArrayInputStream(bytes.toByteArray()));
    }
}
