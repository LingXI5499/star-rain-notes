package com.starrainnotes.boot;

import com.starrainnotes.account.entity.AccountAuditEntity;
import com.starrainnotes.account.mapper.AccountMapper;
import com.starrainnotes.account.service.impl.AccountAuditServiceImpl;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.assertj.core.api.Assertions.assertThat;

/** Normalized servlet sources, used by both audit writes and limiter queries, remain independent. */
class AccountProxyRateLimitTest extends MapperXmlIntegrationSupport {
    /** A's ten failures do not throttle B; arbitrary headers are not read by the audit service. */
    @Test void normalizedSourcesHaveIndependentFailureCounts() {
        try (SqlSession session = openSession()) {
            AccountMapper mapper = session.getMapper(AccountMapper.class);
            AccountAuditServiceImpl audit = new AccountAuditServiceImpl(mapper);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("198.51.100.21");
            request.addHeader("X-Forwarded-For", "203.0.113.77");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
            LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(15);
            long before = mapper.recentAuditCount(request.getRemoteAddr(), "LOGIN_FAILED", since);
            for (int attempt = 0; attempt < 10; attempt++) audit.failed(null, null, "LOGIN_FAILED");
            assertThat(mapper.recentAuditCount("198.51.100.21", "LOGIN_FAILED", since)).isEqualTo(before + 10);
            assertThat(mapper.recentAuditCount("198.51.100.22", "LOGIN_FAILED", since)).isZero();
            assertThat(mapper.recentAuditCount("203.0.113.77", "LOGIN_FAILED", since)).isZero();
            session.rollback();
        } finally { RequestContextHolder.resetRequestAttributes(); }
    }
}
