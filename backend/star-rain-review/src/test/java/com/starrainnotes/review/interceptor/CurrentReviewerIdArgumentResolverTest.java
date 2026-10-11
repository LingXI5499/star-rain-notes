/*
 * CurrentReviewerIdArgumentResolverTest：审核模块的测试文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：CurrentActorApi、ApiException。
 * 阅读入口：annotated（annotated处理入口）；notAnnotated（不Annotated处理入口）；wrongType（wrong类型处理入口）；parameter（parameter处理入口）。
 */

package com.starrainnotes.review.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.exception.ApiException;
import java.lang.reflect.Method;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;

/*
 * @CurrentReviewerId 参数解析器测试。
 *
 * 这是「决策人只能来自认证上下文」的实现点，所以要验证：
 *   1. 只认领标注了注解且类型为 Long 的参数，不干扰其他参数解析；
 *   2. 解析结果恒等于当前认证账户，浏览器无法通过请求体伪造；
 *   3. 未认证时把 CurrentActorApi 的 401（UNAUTHORIZED）原样抛出，
 *      由 GlobalApiExceptionHandler 映射成 401，而不是静默用 null 当决策人。
 */
/**
 * 当前审核人标识ArgumentResolver测试，属于审核模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class CurrentReviewerIdArgumentResolverTest {

    /**
     * 提供当前认证操作者身份的跨模块接口。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi currentActorApi = mock(CurrentActorApi.class);

    /**
     * resolver。
     */
    private final CurrentReviewerIdArgumentResolver resolver =
            new CurrentReviewerIdArgumentResolver(currentActorApi);

    // 用真实 Controller 方法签名做断言，避免测试里另造一个签名而与生产不一致
    /**
     * Sample控制器，属于审核模块。
     * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
     */
    @SuppressWarnings("unused")
    private static final class SampleController {

        /**
         * annotated处理入口。
         *
         * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>
         */
        void annotated(@CurrentReviewerId Long reviewerAccountId) {
        }

        /**
         * 不Annotated处理入口。
         *
         * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>
         */
        void notAnnotated(Long reviewerAccountId) {
        }

        /**
         * wrong类型处理入口。
         *
         * @param reviewerAccountId 审核人账号标识；类型为 <code>String</code>
         */
        void wrongType(@CurrentReviewerId String reviewerAccountId) {
        }
    }

    /**
     * parameter处理入口。
     * 关键处理步骤调用 <code>SampleController.class.getDeclaredMethod</code>。
     *
     * @param methodName method名称；类型为 <code>String</code>
     * @param parameterType parameter类型；类型为 <code>Class&lt;?&gt;</code>
     * @return 类型为 <code>MethodParameter</code> 的处理结果
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    private static MethodParameter parameter(String methodName, Class<?> parameterType) throws Exception {
        Method method = SampleController.class.getDeclaredMethod(methodName, parameterType);
        return new MethodParameter(method, 0);
    }

    /**
     * \u53ea\u8ba4\u9886 @CurrentReviewerId + Long \u7684\u53c2\u6570。
     * 主要协作调用：<code>resolver.supportsParameter</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @DisplayName("只认领 @CurrentReviewerId + Long 的参数")
    void supportsOnlyAnnotatedLongParameter() throws Exception {
        assertThat(resolver.supportsParameter(parameter("annotated", Long.class))).isTrue();
        assertThat(resolver.supportsParameter(parameter("notAnnotated", Long.class))).isFalse();
        assertThat(resolver.supportsParameter(parameter("wrongType", String.class))).isFalse();
    }

    /**
     * \u89e3\u6790\u7ed3\u679c\u5c31\u662f\u5f53\u524d\u8ba4\u8bc1\u8d26\u6237ID\uff0c\u4e0e\u8bf7\u6c42\u53c2\u6570\u65e0\u5173。
     * 主要协作调用：<code>currentActorApi.current</code>、<code>resolver.resolveArgument</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @DisplayName("解析结果就是当前认证账户ID，与请求参数无关")
    void resolvesAuthenticatedAccountId() throws Exception {
        when(currentActorApi.current()).thenReturn(CurrentActorApi.CurrentActor.builder()
                .accountId(42L)
                .roles(Set.of("SUPER_ADMIN"))
                .permissions(Set.of("review:approve"))
                .build());

        Object resolved = resolver.resolveArgument(parameter("annotated", Long.class), null, null, null);

        assertThat(resolved).isEqualTo(42L);
        verify(currentActorApi).current();
    }

    /**
     * \u672a\u8ba4\u8bc1\u65f6\u629b UNAUTHORIZED\uff1a\u4e0d\u80fd\u628a null \u5f53\u6210\u51b3\u7b56\u4eba\u4f20\u4e0b\u53bb。
     * 主要协作调用：<code>currentActorApi.current</code>、<code>resolver.resolveArgument</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @DisplayName("未认证时抛 UNAUTHORIZED：不能把 null 当成决策人传下去")
    void unauthenticatedThrowsUnauthorized() throws Exception {
        when(currentActorApi.current()).thenThrow(new ApiException("UNAUTHORIZED", "请先登录", 401));

        assertThatThrownBy(() -> resolver.resolveArgument(
                parameter("annotated", Long.class), null, null, null))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("UNAUTHORIZED");
    }
}
