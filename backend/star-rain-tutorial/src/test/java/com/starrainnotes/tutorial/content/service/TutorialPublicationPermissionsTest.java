package com.starrainnotes.tutorial.content.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalMatchers.aryEq;
import com.starrainnotes.tutorial.content.controller.*;
import com.starrainnotes.tutorial.content.security.TutorialSecurityContributor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

class TutorialPublicationPermissionsTest {
    @Test void pendingOperationEndpointIsAuthenticatedAndNotPublic() {
        var security = new TutorialSecurityContributor();
        assertTrue(security.authenticatedPatterns().contains("/api/admin/tutorial-operations/pending"));
        assertFalse(security.publicPatterns().contains("/api/admin/tutorial-operations/pending"));
    }
    @Configuration @EnableMethodSecurity static class SecurityConfig {
        @Bean TutorialContentService content() { return mock(TutorialContentService.class); }
        @Bean TutorialPublicationService publication() { return mock(TutorialPublicationService.class); }
        @Bean TutorialOperationService operations() { return mock(TutorialOperationService.class); }
        @Bean TutorialStructureAdminController chapters(TutorialOperationService operations,TutorialContentService content,TutorialPublicationService publication) { return new TutorialStructureAdminController(operations,content,publication); }
        @Bean TutorialPublicationAdminController tutorials(TutorialOperationService operations,TutorialPublicationService publication) { return new TutorialPublicationAdminController(operations,publication); }
    }
    private void actor(String... permissions) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("editor", "", java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
    }
    @Test void publicationEndpointsRequireEditingPermissionAndUseTheReviewGateway() {
        try (var context=new AnnotationConfigApplicationContext(SecurityConfig.class)) {
            actor("tutorial:read-admin");
            var chapters=context.getBean(TutorialStructureAdminController.class);
            var tutorials=context.getBean(TutorialPublicationAdminController.class);
            assertThrows(AccessDeniedException.class,()->chapters.publishChapter(4L));
            assertThrows(AccessDeniedException.class,()->chapters.withdrawChapter(4L));
            assertThrows(AccessDeniedException.class,()->tutorials.publish(1L));
            assertThrows(AccessDeniedException.class,()->tutorials.withdraw(1L));
            actor("tutorial:edit","tutorial:submit");
            tutorials.submit(1L,4L); chapters.publishChapter(4L); chapters.withdrawChapter(4L);
            tutorials.publish(1L); tutorials.withdraw(1L);
            var operations=context.getBean(TutorialOperationService.class);
            verify(operations,times(2)).request(eq("publishChapter"),aryEq(new Object[]{4L}));
            verify(operations).request(eq("withdrawChapter"),aryEq(new Object[]{4L}));
            verify(operations).request(eq("publish"),aryEq(new Object[]{1L}));
            verify(operations).request(eq("withdraw"),aryEq(new Object[]{1L}));
            verifyNoInteractions(context.getBean(TutorialPublicationService.class));
        } finally { SecurityContextHolder.clearContext(); }
    }
}
