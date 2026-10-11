/*
 * TutorialPublicationPermissionsTest：教程与学习模块的测试文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：TutorialSecurityContributor。
 * 阅读入口：pendingOperationEndpointIsAuthenticatedAndNotPublic（待处理操作Endpoint是否已认证及不公开处理入口）；content（内容处理入口）；publication（发布处理入口）；operations（operations处理入口）。
 */

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

/**
 * 教程发布权限集合测试，属于教程与学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class TutorialPublicationPermissionsTest {
    /**
     * 待处理操作Endpoint是否已认证及不公开处理入口。
     * 关键处理步骤调用 <code>security.authenticatedPatterns</code>、<code>security.publicPatterns</code>。
     */
    @Test void pendingOperationEndpointIsAuthenticatedAndNotPublic() {
        var security = new TutorialSecurityContributor();
        assertTrue(security.authenticatedPatterns().contains("/api/admin/tutorial-operations/pending"));
        assertFalse(security.publicPatterns().contains("/api/admin/tutorial-operations/pending"));
    }
    /**
     * 安全配置，属于教程与学习模块。
     * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
     */
    @Configuration @EnableMethodSecurity static class SecurityConfig {
        /**
         * 内容处理入口。
         * @return 类型为 <code>TutorialContentService</code> 的处理结果
         */
        @Bean TutorialContentService content() { return mock(TutorialContentService.class); }
        /**
         * 发布处理入口。
         * @return 类型为 <code>TutorialPublicationService</code> 的处理结果
         */
        @Bean TutorialPublicationService publication() { return mock(TutorialPublicationService.class); }
        /**
         * operations处理入口。
         * @return 类型为 <code>TutorialOperationService</code> 的处理结果
         */
        @Bean TutorialOperationService operations() { return mock(TutorialOperationService.class); }
        /**
         * 章节列表处理入口。
         *
         * @param operations operations；类型为 <code>TutorialOperationService</code>
         * @param content 内容；类型为 <code>TutorialContentService</code>
         * @param publication 发布；类型为 <code>TutorialPublicationService</code>
         * @return 类型为 <code>TutorialStructureAdminController</code> 的处理结果
         */
        @Bean TutorialStructureAdminController chapters(TutorialOperationService operations,TutorialContentService content,TutorialPublicationService publication) { return new TutorialStructureAdminController(operations,content,publication); }
        /**
         * tutorials处理入口。
         *
         * @param operations operations；类型为 <code>TutorialOperationService</code>
         * @param publication 发布；类型为 <code>TutorialPublicationService</code>
         * @return 类型为 <code>TutorialPublicationAdminController</code> 的处理结果
         */
        @Bean TutorialPublicationAdminController tutorials(TutorialOperationService operations,TutorialPublicationService publication) { return new TutorialPublicationAdminController(operations,publication); }
    }
    /**
     * 当前操作对应的身份对象处理入口。
     * 关键处理步骤调用 <code>SecurityContextHolder.getContext</code>、<code>java.util.Arrays.stream</code>。
     *
     * @param permissions 权限集合；类型为 <code>String[]</code>
     */
    private void actor(String... permissions) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("editor", "", java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
    }
    /**
     * 发布Endpoints要求Editing权限及UseThe审核Gateway处理入口。
     * 关键处理步骤调用 <code>actor</code>、<code>context.getBean</code>、<code>chapters.publishChapter</code>、<code>chapters.withdrawChapter</code>。
     */
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
