package com.starrainnotes.tutorial.content.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.starrainnotes.tutorial.content.controller.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

class TutorialPublicationPermissionsTest {
    @Configuration @EnableMethodSecurity static class SecurityConfig {
        @Bean TutorialContentService content() { return mock(TutorialContentService.class); }
        @Bean TutorialPublicationService publication() { return mock(TutorialPublicationService.class); }
        @Bean TutorialStructureAdminController chapters(TutorialContentService content,TutorialPublicationService publication) { return new TutorialStructureAdminController(content,publication); }
        @Bean TutorialPublicationAdminController tutorials(TutorialPublicationService publication) { return new TutorialPublicationAdminController(publication); }
    }
    private void actor(String... permissions) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("editor", "", java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
    }
    @Test void ordinaryEditorCannotPublishOrWithdrawThroughAnyChapterEndpoint() {
        try (var context=new AnnotationConfigApplicationContext(SecurityConfig.class)) {
            actor("tutorial:edit","tutorial:submit");
            var chapters=context.getBean(TutorialStructureAdminController.class);
            var tutorials=context.getBean(TutorialPublicationAdminController.class);
            assertThrows(AccessDeniedException.class,()->chapters.publishChapter(4L));
            assertThrows(AccessDeniedException.class,()->chapters.withdrawChapter(4L));
            assertThrows(AccessDeniedException.class,()->tutorials.publish(1L));
            assertThrows(AccessDeniedException.class,()->tutorials.withdraw(1L));
            tutorials.submit(1L,4L);
            verify(context.getBean(TutorialPublicationService.class)).submitReview(1L,4L);
            actor("tutorial:publish");chapters.publishChapter(4L);
            verify(context.getBean(TutorialPublicationService.class)).publishChapter(4L);
            actor("tutorial:withdraw");chapters.withdrawChapter(4L);
            verify(context.getBean(TutorialPublicationService.class)).withdrawChapter(4L);
        } finally { SecurityContextHolder.clearContext(); }
    }
}
