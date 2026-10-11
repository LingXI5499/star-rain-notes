/*
 * BlogControllerAuthorizationTest：博客模块的测试文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 阅读入口：everyAdminHandlerDeclaresBlogAuthority（\u540e\u53f0\u6bcf\u4e2a\u5904\u7406\u65b9\u6cd5\u90fd\u5fc5\u987b\u58f0\u660e blog:* \u6743\u9650）；publishAndEditUseDistinctPermissions（\u53d1\u5e03\u3001\u64a4\u56de\u3001\u7f16\u8f91\u4f7f\u7528\u5404\u81ea\u72ec\u7acb\u7684\u6743\u9650\u7801\uff0c\u4e0d\u80fd\u5171\u7528\u4e00\u4e2a\u7c97\u7c92\u5ea6\u6743\u9650）；taxonomyEndpointsUseTaxonomyPermission（\u5206\u7c7b\u4e0e\u4e13\u9898\u7ba1\u7406\u7edf\u4e00\u8d70 blog:taxonomy-manage）；publicControllerHasNoPermissionAnnotations（\u524d\u53f0\u516c\u5f00\u63a5\u53e3\u4e0d\u58f0\u660e\u4efb\u4f55\u6743\u9650\u6ce8\u89e3\uff1a\u533f\u540d\u8bbf\u95ee\u662f\u8bbe\u8ba1\u610f\u56fe）。
 */

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
/**
 * 博客控制器Authorization测试，属于博客模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class BlogControllerAuthorizationTest {

    /**
     * CONTROLLERS。
     */
    private static final List<Class<?>> ADMIN_CONTROLLERS = List.of(
            BlogAdminPostController.class,
            BlogAdminTagController.class,
            BlogAdminTopicController.class);

    /**
     * \u540e\u53f0\u6bcf\u4e2a\u5904\u7406\u65b9\u6cd5\u90fd\u5fc5\u987b\u58f0\u660e blog:* \u6743\u9650。
     * 关键处理步骤调用 <code>controller.getDeclaredMethods</code>、<code>Modifier.isPublic</code>、<code>method.getModifiers</code>、<code>method.isSynthetic</code>。
     */
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

    /**
     * \u53d1\u5e03\u3001\u64a4\u56de\u3001\u7f16\u8f91\u4f7f\u7528\u5404\u81ea\u72ec\u7acb\u7684\u6743\u9650\u7801\uff0c\u4e0d\u80fd\u5171\u7528\u4e00\u4e2a\u7c97\u7c92\u5ea6\u6743\u9650。
     * 关键处理步骤调用 <code>expected.put</code>、<code>expected.entrySet</code>、<code>findMethod</code>、<code>entry.getKey</code>。
     */
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

    /**
     * \u5206\u7c7b\u4e0e\u4e13\u9898\u7ba1\u7406\u7edf\u4e00\u8d70 blog:taxonomy-manage。
     * 关键处理步骤调用 <code>BlogAdminTagController.class.getDeclaredMethods</code>、<code>Modifier.isPublic</code>、<code>method.getModifiers</code>、<code>method.isSynthetic</code>。
     */
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

    /**
     * \u524d\u53f0\u516c\u5f00\u63a5\u53e3\u4e0d\u58f0\u660e\u4efb\u4f55\u6743\u9650\u6ce8\u89e3\uff1a\u533f\u540d\u8bbf\u95ee\u662f\u8bbe\u8ba1\u610f\u56fe。
     * 关键处理步骤调用 <code>BlogPublicController.class.getDeclaredMethods</code>、<code>Modifier.isPublic</code>、<code>method.getModifiers</code>、<code>method.isSynthetic</code>。
     */
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

    /**
     * 查找Method。
     * 关键处理步骤调用 <code>type.getDeclaredMethods</code>、<code>method.getName</code>。
     *
     * @param type 类型；类型为 <code>Class&lt;?&gt;</code>
     * @param name 名称；类型为 <code>String</code>
     * @return 类型为 <code>Method</code> 的处理结果
     */
    private static Method findMethod(Class<?> type, String name) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }
}
