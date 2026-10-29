package com.starrainnotes.media.service;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.account.api.PermissionQueryApi;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.entity.MediaAssetEntity;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.remote.RemoteImageFetcher;
import com.starrainnotes.media.service.impl.EditorImageServiceImpl;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EditorImageServiceTest {
    private final MediaAssetMapper assets = mock(MediaAssetMapper.class);
    private final CurrentActorApi actors = mock(CurrentActorApi.class);
    private final PermissionQueryApi permissions = mock(PermissionQueryApi.class);
    private final EditorImageService service = new EditorImageServiceImpl(assets, mock(MediaUploadService.class), actors, permissions, mock(RemoteImageFetcher.class));

    private MediaAssetEntity image(long owner, String access) {
        var image = new MediaAssetEntity();
        image.setId(10L); image.setUploadedByAccountId(owner); image.setAccessLevel(access);
        image.setMediaType("IMAGE"); image.setStatus("ACTIVE"); image.setSha256("fixture");
        when(assets.assetByIdForUpdate(10L)).thenReturn(image);
        return image;
    }

    @Test void insertionMakesOwnImagePublic() {
        image(2L, "PROTECTED");
        when(actors.current()).thenReturn(new CurrentActorApi.CurrentActor(2L, Set.of(), Set.of("media:upload")));
        assertEquals("PUBLIC", service.prepare(10L).getAccessLevel());
        verify(assets).updateAccessLevel(10L, "PUBLIC");
    }
    @Test void insertionCannotExposeAnotherAccountsProtectedImage() {
        image(3L, "PROTECTED");
        when(actors.current()).thenReturn(new CurrentActorApi.CurrentActor(2L, Set.of(), Set.of("media:upload")));
        ApiException error = assertThrows(ApiException.class, () -> service.prepare(10L));
        assertEquals("FORBIDDEN", error.getCode());
        verify(assets, never()).updateAccessLevel(any(), any());
    }
    @Test void publicImagesRemainReusableAndPreparationIsIdempotent() {
        image(3L, "PUBLIC");
        assertEquals("PUBLIC", service.prepare(10L).getAccessLevel());
        verifyNoInteractions(actors);
        verify(assets, never()).updateAccessLevel(any(), any());
    }
    @Test void archivedAndNonImageAssetsCannotBePrepared() {
        var image = image(2L, "PUBLIC"); image.setStatus("ARCHIVED");
        assertThrows(ApiException.class, () -> service.prepare(10L));
        image.setStatus("ACTIVE"); image.setMediaType("DOCUMENT");
        assertThrows(ApiException.class, () -> service.prepare(10L));
    }
}
