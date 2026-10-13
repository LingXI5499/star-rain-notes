package com.starrainnotes.boot;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.media.enumeration.MediaAccessLevel;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.properties.MediaProperties;
import com.starrainnotes.media.security.MediaUploadLimiter;
import com.starrainnotes.media.service.impl.MediaUploadServiceImpl;
import com.starrainnotes.media.service.impl.MediaUploadValidatorImpl;
import com.starrainnotes.media.storage.impl.LocalFileMediaStorageImpl;
import com.starrainnotes.media.vo.MediaAssetVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real MySQL insert plus local file: a commit-time failure must compensate both resources. */
class MediaCommitFailureIntegrationTest extends MapperXmlIntegrationSupport {
    @TempDir Path directory;

    /** Inject a commit failure before durability; Spring performs a confirmed rollback callback. */
    @Test void commitFailureRollsBackAssetAndDeletesFile() throws Exception {
        try (SqlSession session = openSession(); AutoCloseable rollback = () -> session.rollback(true)) {
            MediaProperties properties = new MediaProperties(); properties.setStorageRoot(directory.toString());
            LocalFileMediaStorageImpl storage = new LocalFileMediaStorageImpl(properties);
            MediaAssetMapper mapper = session.getMapper(MediaAssetMapper.class);
            CurrentActorApi actors = mock(CurrentActorApi.class);
            when(actors.current()).thenReturn(CurrentActorApi.CurrentActor.builder().accountId(1L).build());
            MediaUploadServiceImpl service = new MediaUploadServiceImpl(new MediaUploadValidatorImpl(properties),
                    storage, mapper, actors, mock(MediaUploadLimiter.class));
            DataSourceTransactionManager manager = new DataSourceTransactionManager(new SingleConnectionDataSource(session.getConnection(), true)) {
                @Override protected void doCommit(DefaultTransactionStatus status) {
                    throw new TransactionSystemException("isolated simulated commit failure");
                }
            };
            manager.setRollbackOnCommitFailure(true);
            AtomicReference<MediaAssetVO> uploaded = new AtomicReference<>();
            assertThatThrownBy(() -> new TransactionTemplate(manager).execute(status -> {
                uploaded.set(service.upload(new MockMultipartFile("file", "fixture.txt", "text/plain", "isolated text".getBytes(java.nio.charset.StandardCharsets.UTF_8)), MediaAccessLevel.PUBLIC));
                return null;
            })).isInstanceOf(TransactionSystemException.class);
            assertThat(uploaded.get()).isNotNull();
            session.clearCache();
            assertThat(mapper.assetById(uploaded.get().getId())).isNull();
            try (var paths = Files.walk(directory)) { assertThat(paths.filter(Files::isRegularFile).count()).isZero(); }
        }
    }
}
