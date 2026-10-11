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
class StaticPrototypeApiTest {
    private final MediaAssetMapper assets = mock(MediaAssetMapper.class);
    private final MediaStorage storage = mock(MediaStorage.class);
    private final StaticPrototypeApiImpl service = new StaticPrototypeApiImpl(assets, storage);
    private MediaAssetEntity asset;
    @BeforeEach void setup() {
        asset = new MediaAssetEntity(); asset.setId(1L); asset.setStatus("ACTIVE"); asset.setAccessLevel("PUBLIC");
        asset.setFileExtension("zip"); asset.setSizeBytes(100L); asset.setStorageKey("fixture.zip");
        when(assets.assetById(1L)).thenReturn(asset);
    }
    @Test void validStaticAssetsAreServedWithoutExtractingFilesystemPaths() throws Exception {
        archive(Map.of("index.html", "<h1>Prototype</h1>", "assets/app.js", "document.title='Works'"));
        service.validate(1L,"index.html");
        assertThat(new String(service.read(1L,"assets/app.js"),StandardCharsets.UTF_8)).contains("Works");
        assertThat(service.contentType("assets/app.js")).isEqualTo("text/javascript");
        verify(storage, never()).store(any(),any());
    }
    @Test void traversalAndExecutableEntriesAreRejected() throws Exception {
        archive(Map.of("index.html","safe","../escape.js","bad"));
        assertThatThrownBy(() -> service.validate(1L,"index.html")).isInstanceOf(MediaPrototypeInvalidException.class);
        archive(Map.of("index.html","safe","run.exe","bad"));
        assertThatThrownBy(() -> service.validate(1L,"index.html")).isInstanceOf(MediaPrototypeInvalidException.class);
        assertThatThrownBy(() -> service.read(1L,"..%2fsecret")).isInstanceOf(MediaPrototypeInvalidException.class);
        assertThatThrownBy(() -> service.read(1L,"C:/secret.html")).isInstanceOf(MediaPrototypeInvalidException.class);
    }
    @Test void privateArchivedOversizedAndMissingEntryArchivesAreRejected() throws Exception {
        archive(Map.of("index.html","safe"));
        asset.setAccessLevel("PRIVATE"); assertThatThrownBy(() -> service.validate(1L,"index.html")).isInstanceOf(MediaPrototypeInvalidException.class);
        asset.setAccessLevel("PUBLIC"); asset.setStatus("ARCHIVED"); assertThatThrownBy(() -> service.validate(1L,"index.html")).isInstanceOf(MediaPrototypeInvalidException.class);
        asset.setStatus("ACTIVE"); asset.setSizeBytes(26L*1024*1024); assertThatThrownBy(() -> service.validate(1L,"index.html")).isInstanceOf(MediaPrototypeInvalidException.class);
        asset.setSizeBytes(100L); assertThatThrownBy(() -> service.validate(1L,"missing.html")).isInstanceOf(MediaPrototypeInvalidException.class);
    }
    @Test void expandedFileLimitsAreMeasuredRatherThanTrustingZipHeaders() throws Exception {
        archive(Map.of("index.html","a".repeat(10*1024*1024+1)));
        assertThatThrownBy(() -> service.validate(1L,"index.html")).isInstanceOf(MediaPrototypeInvalidException.class);
    }
    private void archive(Map<String,String> entries) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            for (var item : entries.entrySet()) { zip.putNextEntry(new ZipEntry(item.getKey())); zip.write(item.getValue().getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); }
        }
        when(storage.open("fixture.zip")).thenAnswer(i -> new ByteArrayInputStream(bytes.toByteArray()));
    }
}
