package com.starrainnotes.blog.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/*
 * 后台接口的权限声明测试（越权拒绝的第一层证据）。
 *
 * 真正的拒绝由 Spring Security 在运行时执行，但“某个端点忘了写 @PreAuthorize”
 * 这种疏漏只有在线上才会暴露。这里用反射把三类后台 Controller 的每个处理方法
 * 都过一遍：没有声明权限、或者声明了非 blog 权限，测试就失败。
 *
 * 注意这只覆盖声明层；服务层的角色约束（发布必须 SUPER_ADMIN）在
 * BlogPublishServiceImplTest 里单独验证，两者互补。
 */
class BlogControllerAuthorizationTest {

    private static final List<Class<?>> ADMIN_CONTROLLERS = List.of(
            BlogAdminPostController.class,
            BlogAdminTagController.class,
            BlogAdminTopicController.class);

    @Test
    @DisplayName("后台每个处理方法都必须声明 blog:* 权限")
    void everyAdminHandlerDeclaresBlogAuthority() {
        for (Class<?> controller : ADMIN_CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic()) {
                    continue;
                }
                PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
                assertThat(annotation)
                        .as("%s.%s 必须声明 @PreAuthorize，否则任何登录用户都能调用",
                                controller.getSimpleName(), method.getName())
                        .isNotNull();
                assertThat(annotation.value())
                        .as("%s.%s 必须要求 blog:* 权限", controller.getSimpleName(), method.getName())
                        .contains("hasAuthority('blog:");
            }
        }
    }

    @Test
    @DisplayName("发布、撤回、编辑使用各自独立的权限码，不能共用一个粗粒度权限")
    void publishAndEditUseDistinctPermissions() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("publish", "blog:publish");
        expected.put("withdraw", "blog:withdraw");
        expected.put("create", "blog:edit");
        expected.put("update", "blog:edit");
        expected.put("updateBody", "blog:edit");
        expected.put("delete", "blog:edit");
        expected.put("restore", "blog:publish");

        for (Map.Entry<String, String> entry : expected.entrySet()) {
            Method method = findMethod(BlogAdminPostController.class, entry.getKey());
            assertThat(method).as("BlogAdminPostController 缺少方法 %s", entry.getKey()).isNotNull();
            PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).contains("'" + entry.getValue() + "'");
        }
    }

    @Test
    @DisplayName("分类与专题管理统一走 blog:taxonomy-manage")
    void taxonomyEndpointsUseTaxonomyPermission() {
        for (Method method : BlogAdminTagController.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic()) {
                continue;
            }
            String value = method.getAnnotation(PreAuthorize.class).value();
            if (method.getName().equals("page")) {
                assertThat(value).contains("blog:read-admin");
            } else {
                assertThat(value).contains("blog:taxonomy-manage");
            }
        }
        PreAuthorize reorder = findMethod(BlogAdminTopicController.class, "reorder").getAnnotation(PreAuthorize.class);
        assertThat(reorder.value()).contains("blog:taxonomy-manage");
    }

    @Test
    @DisplayName("前台公开接口不声明任何权限注解：匿名访问是设计意图")
    void publicControllerHasNoPermissionAnnotations() {
        for (Method method : BlogPublicController.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic()) {
                continue;
            }
            assertThat(method.getAnnotation(PreAuthorize.class))
                    .as("前台接口 %s 不应要求权限", method.getName())
                    .isNull();
        }
    }

    private static Method findMethod(Class<?> type, String name) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }
}
