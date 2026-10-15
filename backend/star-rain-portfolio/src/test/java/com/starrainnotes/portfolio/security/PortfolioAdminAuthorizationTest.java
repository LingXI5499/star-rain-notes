package com.starrainnotes.portfolio.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.starrainnotes.portfolio.controller.PortfolioAdminController;
import com.starrainnotes.portfolio.dto.WorkPatchDTO;
import com.starrainnotes.portfolio.service.PortfolioWorkService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/** Exercise the actual controller permission interceptors without a database or new dependencies. */
@SpringJUnitConfig(PortfolioAdminAuthorizationTest.Config.class)
class PortfolioAdminAuthorizationTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean PortfolioWorkService works() { return mock(PortfolioWorkService.class); }
        @Bean PortfolioAdminController controller(PortfolioWorkService works) { return new PortfolioAdminController(works); }
    }

    @Autowired private PortfolioAdminController controller;
    @Autowired private PortfolioWorkService works;

    @BeforeEach void resetService() { reset(works); }
    @AfterEach void clearIdentity() { SecurityContextHolder.clearContext(); }

    @Test void ordinaryUserCannotEditPublishRestoreOrRemoveCover() {
        authenticate("ROLE_USER");
        assertThatThrownBy(() -> controller.update(9L, WorkPatchDTO.builder().title("修改").build()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.publish(9L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.restore(9L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.removeMedia(3L)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(works);
    }

    @Test void editPermissionDoesNotGrantPublication() {
        authenticate("portfolio:edit");
        controller.removeMedia(3L);
        verify(works).removeMedia(3L);
        assertThatThrownBy(() -> controller.publish(9L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test void publicationPermissionAllowsTheExistingPublishWorkflow() {
        authenticate("portfolio:publish");
        controller.publish(9L);
        verify(works).publish(9L);
    }

    private void authenticate(String authority) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test-user", "unused", List.of(new SimpleGrantedAuthority(authority))));
    }
}
