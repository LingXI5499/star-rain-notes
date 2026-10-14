package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import com.starrainnotes.common.web.ContentVersion;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.seo.config.CanonicalUrlResolver;
import com.starrainnotes.seo.config.SeoProperties;
import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.provider.SeoSourceProvider;
import com.starrainnotes.seo.renderer.SeoHtmlRenderer;
import com.starrainnotes.seo.service.impl.SeoSnapshotServiceImpl;
import com.starrainnotes.media.controller.MediaContentController;
import com.starrainnotes.media.service.MediaAccessService;
import com.starrainnotes.media.vo.MediaContentVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class AuditPublicationSafetyTest {
    @AfterEach void clear() { RequestContextHolder.resetRequestAttributes(); }

    @Test void staleEditorCannotOverwriteTheLockedCurrentVersion() {
        var request = new MockHttpServletRequest("PUT", "/api/admin/blog/posts/7/body");
        request.addHeader("If-Match", "\"4\"");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        assertDoesNotThrow(() -> ContentVersion.require(4L));
        assertEquals(409, assertThrows(ApiException.class, () -> ContentVersion.require(5L)).getStatus());
    }

    @Test void withdrawalBlocksAStaleActiveSnapshotEvenWhenRemovalFailed() {
        var provider = mock(SeoSourceProvider.class); var mapper = mock(SeoPageMapper.class);
        when(provider.supports("/blog/posts/secret")).thenReturn(true);
        when(provider.publiclyVisible("/blog/posts/secret")).thenReturn(false);
        var properties = new SeoProperties(); properties.setPublicBaseUrl("https://example.org");
        var service = new SeoSnapshotServiceImpl(List.of(provider), mapper, new CanonicalUrlResolver(properties), new SeoHtmlRenderer());
        assertTrue(service.getByRoute("/blog/posts/secret").isEmpty());
        assertTrue(service.getMetaByRoute("/blog/posts/secret").isEmpty());
        verifyNoInteractions(mapper);
        when(provider.publiclyVisible("/blog/posts/secret")).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> service.getByRoute("/blog/posts/secret"));
        verifyNoInteractions(mapper);
    }

    @Test void conditionalReadAuthorizesWithoutOpeningAStream() {
        var request = new MockHttpServletRequest("GET", "/api/media/assets/7/content");
        request.addHeader("If-None-Match", "\"sha\"");
        var response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        var access = mock(MediaAccessService.class);
        when(access.describe(7L)).thenReturn(MediaContentVO.builder().sha256("sha").accessLevel("PUBLIC").build());
        assertEquals(304, new MediaContentController(access).content(7L, null).getStatusCode().value());
        verify(access, never()).open(any(), any());
    }

    @Test void responseVersionTracksOnlyThisCommittedWriteAndIsAbsentOnFailure() {
        var request = new MockHttpServletRequest("PUT", "/api/admin/blog/posts/7/body");
        request.addHeader("If-Match", "\"4\"");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ContentVersion.require(4L); ContentVersion.advanced(); ContentVersion.advanced();
        var advice = new com.starrainnotes.boot.reliability.ContentVersionResponseAdvice();
        var success = new MockHttpServletResponse();
        var successfulResponse = new org.springframework.http.server.ServletServerHttpResponse(success);
        advice.beforeBodyWrite(null, null, null, null, null, successfulResponse);
        assertEquals("6", successfulResponse.getHeaders().getFirst("X-Content-Version"));
        var failure = new MockHttpServletResponse(); failure.setStatus(500);
        advice.beforeBodyWrite(null, null, null, null, null, new org.springframework.http.server.ServletServerHttpResponse(failure));
        assertNull(failure.getHeader("X-Content-Version"));
    }
}
