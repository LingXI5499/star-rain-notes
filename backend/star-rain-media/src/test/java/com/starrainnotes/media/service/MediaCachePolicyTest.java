package com.starrainnotes.media.service;

import com.starrainnotes.media.controller.MediaContentController;
import com.starrainnotes.media.enumeration.MediaType;
import com.starrainnotes.media.vo.MediaContentVO;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Public copies may be cached locally but must recheck mutable access before reuse. */
class MediaCachePolicyTest {
    @Test void publicRequiresRevalidationAndProtectedIsNotStored() {
        MediaAccessService access = mock(MediaAccessService.class);
        when(access.open(1L, null)).thenReturn(content("PUBLIC"));
        when(access.open(2L, null)).thenReturn(content("PROTECTED"));
        MediaContentController controller = new MediaContentController(access);
        var publicResponse = controller.content(1L, null);
        assertThat(publicResponse.getHeaders().getCacheControl()).contains("private", "no-cache", "must-revalidate");
        assertThat(publicResponse.getHeaders().getCacheControl()).doesNotContain("no-store");
        assertThat(publicResponse.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(publicResponse.getHeaders().getETag()).isEqualTo("\"digest\"");
        assertThat(controller.content(2L, null).getHeaders().getCacheControl()).contains("private", "no-store");
    }
    private MediaContentVO content(String access) {
        return new MediaContentVO(1L, MediaType.IMAGE, "image/png", "fixture.png", 1L,
                "digest", access, null, new ByteArrayInputStream(new byte[]{1}));
    }
}
