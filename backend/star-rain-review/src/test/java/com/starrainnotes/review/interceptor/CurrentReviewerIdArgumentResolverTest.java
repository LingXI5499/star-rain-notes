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
class CurrentReviewerIdArgumentResolverTest {

    private final CurrentActorApi currentActorApi = mock(CurrentActorApi.class);

    private final CurrentReviewerIdArgumentResolver resolver =
            new CurrentReviewerIdArgumentResolver(currentActorApi);

    // 用真实 Controller 方法签名做断言，避免测试里另造一个签名而与生产不一致
    @SuppressWarnings("unused")
    private static final class SampleController {

        void annotated(@CurrentReviewerId Long reviewerAccountId) {
        }

        void notAnnotated(Long reviewerAccountId) {
        }

        void wrongType(@CurrentReviewerId String reviewerAccountId) {
        }
    }

    private static MethodParameter parameter(String methodName, Class<?> parameterType) throws Exception {
        Method method = SampleController.class.getDeclaredMethod(methodName, parameterType);
        return new MethodParameter(method, 0);
    }

    @Test
    @DisplayName("只认领 @CurrentReviewerId + Long 的参数")
    void supportsOnlyAnnotatedLongParameter() throws Exception {
        assertThat(resolver.supportsParameter(parameter("annotated", Long.class))).isTrue();
        assertThat(resolver.supportsParameter(parameter("notAnnotated", Long.class))).isFalse();
        assertThat(resolver.supportsParameter(parameter("wrongType", String.class))).isFalse();
    }

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
