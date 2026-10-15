package com.starrainnotes.boot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.delivery.MailOutbox;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Real Controller → Security → Service → MyBatis → isolated MySQL; only SMTP transport is replaced. */
@SpringBootTest(properties = {
        "spring.config.import=", "star-rain.account.smtp-enabled=true",
        "management.health.mail.enabled=false",
        "star-rain.account.mail-from=account-ci@example.test",
        "star-rain.account.mail-initial-delay-ms=3600000",
        "star-rain.account.frontend-origin=https://account-ci.example.test",
        "STAR_RAIN_MAIL_OUTBOX_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "star-rain.account.throttle.key=isolated-account-v3-integration-key-only",
        "STAR_RAIN_BOOTSTRAP_ADMIN_ENABLED=false", "STAR_RAIN_INDEXNOW_ENABLED=false",
        "STAR_RAIN_MEDIA_ORPHAN_CLEANUP=false"
})
@AutoConfigureMockMvc(print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@EnabledIfEnvironmentVariable(named = "STAR_RAIN_DB_URL", matches = ".*/star_rain_(ci|account_ci)(\\?.*)?")
class AccountV3IntegrationTest {
    private static final String SCHEMA = "sr_account_v3_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final String PASSWORD = "Isolated-Test-Pass-5499!";
    private static final String NEW_PASSWORD = "Isolated-New-Pass-5499!";
    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static boolean created;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MailOutbox outbox;
    @MockitoBean private JavaMailSender smtp;
    private final List<MimeMessage> delivered = new CopyOnWriteArrayList<>();
    private boolean smtpFailure;

    /** Never uses the repository's private configuration, and never migrates the supplied base schema. */
    @DynamicPropertySource
    static void isolatedSchema(DynamicPropertyRegistry registry) throws Exception {
        String base = System.getenv("STAR_RAIN_DB_URL");
        if (base == null || !base.matches("jdbc:mysql://[^/]+/star_rain_(ci|account_ci)(\\?.*)?")) {
            throw new IllegalStateException("Account integration requires an explicitly isolated CI database");
        }
        try (var connection = DriverManager.getConnection(base, System.getenv("STAR_RAIN_DB_USERNAME"),
                System.getenv("STAR_RAIN_DB_PASSWORD")); var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + SCHEMA + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            created = true;
        }
        registry.add("spring.datasource.url", () -> base.replaceFirst("/star_rain_(ci|account_ci)(?=\\?|$)", "/" + SCHEMA));
        registry.add("spring.datasource.username", () -> System.getenv("STAR_RAIN_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> System.getenv("STAR_RAIN_DB_PASSWORD"));
    }

    /** Drops only the random schema this class successfully created. */
    @AfterAll
    static void removeSchema() throws Exception {
        if (!created || !SCHEMA.matches("sr_account_v3_test_[0-9a-f]{32}")) return;
        try (var connection = DriverManager.getConnection(System.getenv("STAR_RAIN_DB_URL"),
                System.getenv("STAR_RAIN_DB_USERNAME"), System.getenv("STAR_RAIN_DB_PASSWORD"));
                var statement = connection.createStatement()) {
            statement.execute("DROP DATABASE " + SCHEMA);
        }
    }

    /** Actual MIME creation is retained, including UTF-8 content and encrypted outbox round trips. */
    @BeforeEach
    void transport() {
        smtpFailure = false;
        // This class owns a random schema; leftover fixture jobs must not couple test outcomes.
        jdbc.update("DELETE FROM sr_mail_outbox");
        when(smtp.createMimeMessage()).thenAnswer(call -> new MimeMessage(Session.getInstance(new Properties())));
        doAnswer(call -> {
            if (smtpFailure) throw new MailSendException("isolated SMTP outage");
            delivered.add(call.getArgument(0));
            return null;
        }).when(smtp).send(any(MimeMessage.class));
    }

    /** AUTH-01/02/03/10/11/19/22: public registration and exact security protocol responses. */
    @Test
    void registrationEnumerationSessionCsrfAndPublicAccess() throws Exception {
        Client client = new Client();
        String name = name(), email = name + "@example.test";
        JsonNode fresh = body(client.send("POST", "/api/auth/register/verification-codes", Map.of("email", email)));
        assertThat(fresh.path("data").has("existingAccount")).isFalse();
        outbox.deliver();
        String code = mailValue(email, "(\\d{6})");
        MvcResult registered = client.send("POST", "/api/auth/register", registration(name, email, code));
        assertThat(registered.getResponse().getStatus()).isEqualTo(201);
        assertThat(body(registered).path("data").path("roles").toString()).contains("USER").doesNotContain("ADMIN");
        jdbc.update("UPDATE sr_email_verification SET sent_at=DATE_SUB(UTC_TIMESTAMP(3),INTERVAL 61 SECOND) WHERE email=?", email);
        MvcResult existing = client.send("POST", "/api/auth/register/verification-codes", Map.of("email", email));
        assertThat(body(existing)).isEqualTo(fresh);
        assertThat(existing.getResponse().getStatus()).isEqualTo(200);
        outbox.deliver();
        assertThat(client.send("POST", "/api/auth/register", registration(name(), email, mailValue(email, "(\\d{6})")))
                .getResponse().getStatus()).isEqualTo(409);
        String sessionBefore = client.session.getId();
        assertThat(client.login(name, PASSWORD).getResponse().getStatus()).isEqualTo(200);
        assertThat(client.session.getId()).isNotEqualTo(sessionBefore);
        assertThat(client.get("/api/account/me").getResponse().getStatus()).isEqualTo(200);
        assertThat(client.get("/api/admin/accounts").getResponse().getStatus()).isEqualTo(403);
        assertThat(client.raw("PATCH", "/api/account/me", Map.of("displayName", "denied"), false).getResponse().getStatus()).isEqualTo(403);
        assertThat(client.send("POST", "/api/auth/logout", Map.of()).getResponse().getStatus()).isEqualTo(200);
        assertThat(client.get("/api/account/me").getResponse().getStatus()).isEqualTo(401);
        for (String path : List.of("/api/public/site/config", "/api/public/blog/posts", "/api/public/tutorials", "/api/public/portfolio/works")) {
            assertThat(client.get(path).getResponse().getStatus()).as(path).isEqualTo(200);
        }
        assertThat(client.send("POST", "/api/auth/register/verification-codes", Map.of("email", "invalid"))
                .getResponse().getStatus()).isEqualTo(400);
    }

    /** AUTH-02/04: verification is mandatory before identity conflicts, and consumed atomically. */
    @Test
    void verificationExpiryFailureBudgetAndConcurrentConsumption() throws Exception {
        Client client = new Client();
        String name = name(), email = name + "@example.test";
        client.send("POST", "/api/auth/register/verification-codes", Map.of("email", email)); outbox.deliver();
        String code = mailValue(email, "(\\d{6})"), wrong = code.equals("000000") ? "111111" : "000000";
        for (int index = 0; index < 5; index++) {
            assertThat(client.send("POST", "/api/auth/register", registration(name, email, wrong)).getResponse().getStatus()).isEqualTo(400);
        }
        assertThat(client.send("POST", "/api/auth/register", registration(name, email, code)).getResponse().getStatus()).isEqualTo(429);
        jdbc.update("UPDATE sr_email_verification SET failed_attempts=0,expires_at=DATE_SUB(UTC_TIMESTAMP(3),INTERVAL 1 SECOND) WHERE email=?", email);
        assertThat(client.send("POST", "/api/auth/register", registration(name, email, code)).getResponse().getStatus()).isEqualTo(400);
        jdbc.update("UPDATE sr_email_verification SET expires_at=DATE_ADD(UTC_TIMESTAMP(3),INTERVAL 10 MINUTE) WHERE email=?", email);
        var statuses = concurrent(2, index -> new Client().send("POST", "/api/auth/register", registration(name, email, code)).getResponse().getStatus());
        assertThat(statuses.stream().filter(status -> status == 201).count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sr_account WHERE email=?", Integer.class, email)).isEqualTo(1);
        assertThat(client.send("POST", "/api/auth/register", registration(name(), email, code)).getResponse().getStatus()).isEqualTo(409);
    }

    /** AUTH-06/07/08/09: concurrent aliases share a budget; forged headers never change its source. */
    @Test
    void atomicLoginBudgetsAcrossIpsAndIdentifiersRecover() throws Exception {
        String name = register(), email = name + "@example.test";
        List<Integer> statuses = concurrent(16, index -> new Client().login(index % 2 == 0 ? name.toUpperCase() : email, "incorrect").getResponse().getStatus());
        assertThat(statuses.stream().filter(status -> status == 401).count()).isEqualTo(10);
        assertThat(statuses.stream().filter(status -> status == 429).count()).isEqualTo(6);
        assertThat(new Client().login(email, PASSWORD).getResponse().getStatus()).isEqualTo(429);
        jdbc.update("UPDATE sr_login_throttle SET window_started_at=DATE_SUB(UTC_TIMESTAMP(3),INTERVAL 16 MINUTE)");
        assertThat(new Client().login(email, PASSWORD).getResponse().getStatus()).isEqualTo(200);
        Client source = new Client();
        source.login("missing-one", "incorrect");
        // The isolated fixture shortens the remaining source budget without spending 100 BCrypt calls.
        jdbc.update("UPDATE sr_login_throttle SET attempts=100 WHERE attempts=1 AND window_started_at>DATE_SUB(UTC_TIMESTAMP(3),INTERVAL 1 MINUTE)");
        assertThat(source.login("missing-two", "incorrect").getResponse().getStatus()).isEqualTo(429);
        assertThat(new Client().login("missing-two", "incorrect").getResponse().getStatus()).isEqualTo(401);
    }

    /** AUTH-05/12/13/14: retries retain a single token, concurrent confirmation has one winner. */
    @Test
    void resetGenericRequestSmtpRetryConcurrentUseAndSessionInvalidation() throws Exception {
        String name = register(), email = name + "@example.test";
        Client loggedIn = new Client(); loggedIn.login(name, PASSWORD);
        Client requester = new Client();
        MvcResult known = requester.send("POST", "/api/auth/password-reset/request", Map.of("email", email));
        MvcResult unknown = requester.send("POST", "/api/auth/password-reset/request", Map.of("email", name() + "@example.test"));
        assertThat(known.getResponse().getStatus()).isEqualTo(200);
        assertThat(unknown.getResponse().getContentAsString()).isEqualTo(known.getResponse().getContentAsString());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sr_password_reset WHERE account_id=?", Integer.class, id(name))).isZero();
        smtpFailure = true; outbox.deliver();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sr_mail_outbox WHERE attempts=1", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sr_password_reset WHERE account_id=?", Integer.class, id(name))).isEqualTo(1);
        smtpFailure = false;
        jdbc.update("UPDATE sr_mail_outbox SET next_attempt_at=UTC_TIMESTAMP(3)"); outbox.deliver();
        String token = mailValue(email, "token=([A-Za-z0-9_-]+)");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sr_password_reset WHERE account_id=?", Integer.class, id(name))).isEqualTo(1);
        List<Integer> statuses = concurrent(2, index -> new Client().send("POST", "/api/auth/password-reset/confirm",
                Map.of("token", token, "newPassword", NEW_PASSWORD, "confirmPassword", NEW_PASSWORD)).getResponse().getStatus());
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(loggedIn.get("/api/account/me").getResponse().getStatus()).isEqualTo(401);
        assertThat(new Client().login(name, PASSWORD).getResponse().getStatus()).isEqualTo(401);
        assertThat(new Client().login(email, NEW_PASSWORD).getResponse().getStatus()).isEqualTo(200);
        assertThat(requester.send("POST", "/api/auth/password-reset/confirm",
                Map.of("token", "invalid", "newPassword", PASSWORD, "confirmPassword", PASSWORD)).getResponse().getStatus()).isEqualTo(400);
    }

    /** AUTH-12/15/16/17/18/22: roles and status invalidate all sessions; last super admin stays active. */
    @Test
    void accountGovernanceAndPasswordChangeRespectRoleBoundaries() throws Exception {
        String superName = register(), userName = register();
        long superId = id(superName), userId = id(userName);
        jdbc.update("INSERT INTO sr_account_role(account_id,role_id) SELECT ?,id FROM sr_role WHERE code='SUPER_ADMIN'", superId);
        Client admin = new Client(); admin.login(superName, PASSWORD);
        assertThat(admin.send("PATCH", "/api/admin/accounts/" + superId + "/status", Map.of("status", "DISABLED"))
                .getResponse().getStatus()).isEqualTo(409);
        Client user = new Client(); user.login(userName, PASSWORD);
        assertThat(user.send("PATCH", "/api/admin/accounts/" + superId + "/status", Map.of("status", "DISABLED"))
                .getResponse().getStatus()).isEqualTo(403);
        MvcResult invitation = admin.send("POST", "/api/admin/account-invitations", Map.of("targetAccountId", String.valueOf(userId)));
        assertThat(invitation.getResponse().getStatus()).isEqualTo(200);
        String invitationId = body(invitation).path("data").path("id").asText();
        assertThat(user.send("POST", "/api/account/invitations/" + invitationId + "/accept", Map.of()).getResponse().getStatus()).isEqualTo(200);
        assertThat(user.get("/api/account/me").getResponse().getStatus()).isEqualTo(401);
        user.login(userName, PASSWORD);
        assertThat(body(user.get("/api/account/me")).path("data").path("roles").toString()).contains("ADMIN");
        assertThat(user.get("/api/admin/accounts").getResponse().getStatus()).isEqualTo(403);
        assertThat(admin.send("POST", "/api/admin/accounts/" + userId + "/revoke-admin", Map.of()).getResponse().getStatus()).isEqualTo(200);
        assertThat(user.get("/api/account/me").getResponse().getStatus()).isEqualTo(401);
        user.login(userName, PASSWORD);
        admin.send("PATCH", "/api/admin/accounts/" + userId + "/status", Map.of("status", "DISABLED"));
        assertThat(user.get("/api/account/me").getResponse().getStatus()).isEqualTo(401);
        assertThat(new Client().login(userName, PASSWORD).getResponse().getStatus()).isEqualTo(401);
        admin.send("PATCH", "/api/admin/accounts/" + userId + "/status", Map.of("status", "ACTIVE"));
        user.login(userName, PASSWORD); Client peer = new Client(); peer.login(userName, PASSWORD);
        assertThat(user.send("PUT", "/api/account/me/password", Map.of("currentPassword", PASSWORD,
                "newPassword", NEW_PASSWORD, "confirmPassword", NEW_PASSWORD)).getResponse().getStatus()).isEqualTo(200);
        assertThat(peer.get("/api/account/me").getResponse().getStatus()).isEqualTo(401);
        assertThat(new Client().login(userName, PASSWORD).getResponse().getStatus()).isEqualTo(401);
        assertThat(new Client().login(userName, NEW_PASSWORD).getResponse().getStatus()).isEqualTo(200);
        // Reissue rotates the old token, expired/revoked invitations never grant a role.
        String targetName = register(); long targetId = id(targetName);
        MvcResult pending = admin.send("POST", "/api/admin/account-invitations", Map.of("targetAccountId", String.valueOf(targetId)));
        String pendingId = body(pending).path("data").path("id").asText();
        jdbc.update("UPDATE sr_admin_invitation SET expires_at=DATE_SUB(UTC_TIMESTAMP(3),INTERVAL 1 SECOND),last_sent_at=DATE_SUB(UTC_TIMESTAMP(3),INTERVAL 61 SECOND) WHERE id=?", pendingId);
        Client target = new Client(); target.login(targetName, PASSWORD);
        assertThat(target.send("POST", "/api/account/invitations/" + pendingId + "/accept", Map.of()).getResponse().getStatus()).isEqualTo(409);
        assertThat(admin.send("POST", "/api/admin/account-invitations/" + pendingId + "/resend", Map.of()).getResponse().getStatus()).isEqualTo(200);
        assertThat(admin.send("POST", "/api/admin/account-invitations/" + pendingId + "/revoke", Map.of()).getResponse().getStatus()).isEqualTo(200);
        assertThat(target.send("POST", "/api/account/invitations/" + pendingId + "/accept", Map.of()).getResponse().getStatus()).isEqualTo(409);
    }

    /** AUTH-14: expired tokens do not change credentials; request limits also count unknown addresses. */
    @Test
    void expiredResetAndUnknownAddressCooldownDoNotRevealIdentity() throws Exception {
        String name = register(), email = name + "@example.test"; Client client = new Client();
        client.send("POST", "/api/auth/password-reset/request", Map.of("email", email)); outbox.deliver();
        String token = mailValue(email, "token=([A-Za-z0-9_-]+)");
        jdbc.update("UPDATE sr_password_reset SET expires_at=DATE_SUB(UTC_TIMESTAMP(3),INTERVAL 1 SECOND) WHERE account_id=?", id(name));
        assertThat(client.send("POST", "/api/auth/password-reset/confirm", Map.of("token", token,
                "newPassword", NEW_PASSWORD, "confirmPassword", NEW_PASSWORD)).getResponse().getStatus()).isEqualTo(409);
        assertThat(new Client().login(name, PASSWORD).getResponse().getStatus()).isEqualTo(200);
        String unknown = name() + "@example.test";
        for (int index = 0; index < 3; index++) {
            assertThat(client.send("POST", "/api/auth/password-reset/request", Map.of("email", unknown)).getResponse().getStatus()).isEqualTo(200);
        }
        assertThat(new Client().send("POST", "/api/auth/password-reset/request", Map.of("email", unknown)).getResponse().getStatus()).isEqualTo(429);
    }

    /** AUTH-20: object IDs and forged owner query parameters cannot cross the tutorial ownership boundary. */
    @Test
    void tutorialPlansAndSessionsRejectAnotherAccount() throws Exception {
        String ownerName = register(), otherName = register(); long ownerId = id(ownerName);
        Client other = new Client(); other.login(otherName, PASSWORD);
        jdbc.update("INSERT INTO sr_study_plan(account_id,tutorial_id,name,status) VALUES(?,900000001,'Private plan','DRAFT')", ownerId);
        Long plan = jdbc.queryForObject("SELECT id FROM sr_study_plan WHERE account_id=?", Long.class, ownerId);
        jdbc.update("INSERT INTO sr_learning_session(account_id,session_type,tutorial_id,chapter_id,status) VALUES(?,'INITIAL_STUDY',900000001,900000002,'IN_PROGRESS')", ownerId);
        Long session = jdbc.queryForObject("SELECT id FROM sr_learning_session WHERE account_id=?", Long.class, ownerId);
        for (String path : List.of("/api/account/learning/plans/" + plan,
                "/api/account/learning/plans/" + plan + "/tasks", "/api/account/learning/sessions/" + session)) {
            assertThat(other.get(path + "?accountId=" + ownerId).getResponse().getStatus()).isIn(403, 404);
        }
        assertThat(other.send("POST", "/api/account/learning/plans/" + plan + "/cancel", Map.of()).getResponse().getStatus()).isIn(403, 404);
        assertThat(jdbc.queryForObject("SELECT status FROM sr_study_plan WHERE id=?", String.class, plan)).isEqualTo("DRAFT");
        Client visitor = new Client();
        assertThat(visitor.get("/api/account/learning/plans").getResponse().getStatus()).isEqualTo(401);
        assertThat(visitor.get("/api/account/english/vocabulary/memory").getResponse().getStatus()).isEqualTo(401);
    }

    /** AUTH-21: private writing resource and revisions remain isolated through the actual controllers. */
    @Test
    void privateWritingCannotBeReadModifiedOrDeletedByAnotherUser() throws Exception {
        Client owner = new Client(), other = new Client(); owner.login(register(), PASSWORD); other.login(register(), PASSWORD);
        MvcResult createdArticle = owner.send("POST", "/api/account/english/writing/articles", Map.of("bodyMarkdown", "Private original"));
        assertThat(createdArticle.getResponse().getStatus()).isEqualTo(200);
        String id = body(createdArticle).path("data").path("id").asText();
        for (String suffix : List.of("", "/revisions")) {
            assertThat(other.get("/api/account/english/writing/articles/" + id + suffix).getResponse().getStatus()).isIn(403, 404);
        }
        assertThat(other.send("PUT", "/api/account/english/writing/articles/" + id,
                Map.of("bodyMarkdown", "stolen", "rowVersion", 0)).getResponse().getStatus()).isIn(403, 404);
        assertThat(other.send("DELETE", "/api/account/english/writing/articles/" + id, Map.of()).getResponse().getStatus()).isIn(403, 404);
        assertThat(body(owner.get("/api/account/english/writing/articles/" + id)).path("data").path("bodyMarkdown").asText()).isEqualTo("Private original");
    }

    /** Creates a normal USER through the entire email-code registration workflow. */
    private String register() throws Exception {
        String name = name(), email = name + "@example.test"; Client client = new Client();
        assertThat(client.send("POST", "/api/auth/register/verification-codes", Map.of("email", email)).getResponse().getStatus()).isEqualTo(200);
        outbox.deliver();
        assertThat(client.send("POST", "/api/auth/register", registration(name, email, mailValue(email, "(\\d{6})")))
                .getResponse().getStatus()).isEqualTo(201);
        return name;
    }

    private static String name() { return "v3_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16); }
    private long id(String name) { return jdbc.queryForObject("SELECT id FROM sr_account WHERE username=?", Long.class, name); }
    private static Map<String, String> registration(String name, String email, String code) {
        return Map.of("username", name, "email", email, "verificationCode", code, "password", PASSWORD, "confirmPassword", PASSWORD);
    }
    private JsonNode body(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsByteArray()); }

    /** Secrets stay within test memory and are never printed or written into reports. */
    private String mailValue(String email, String pattern) throws Exception {
        for (int index = delivered.size() - 1; index >= 0; index--) {
            MimeMessage message = delivered.get(index);
            if (!message.getAllRecipients()[0].toString().equals(email)) continue;
            var match = Pattern.compile(pattern).matcher(message.getContent().toString());
            if (match.find()) return match.group(1);
        }
        throw new AssertionError("Expected isolated test mail was not delivered");
    }

    /** Releases actual HTTP requests simultaneously, not a sequence of mocked mapper invocations. */
    private List<Integer> concurrent(int count, CheckedRequest operation) throws Exception {
        CountDownLatch ready = new CountDownLatch(count), start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(count)) {
            var futures = new ArrayList<java.util.concurrent.Future<Integer>>();
            for (int index = 0; index < count; index++) {
                int n = index;
                futures.add(pool.submit(() -> { ready.countDown(); start.await(); return operation.run(n); }));
            }
            ready.await(); start.countDown(); var statuses = new ArrayList<Integer>();
            for (var future : futures) statuses.add(future.get(60, java.util.concurrent.TimeUnit.SECONDS));
            return statuses;
        }
    }

    @FunctionalInterface
    private interface CheckedRequest { int run(int index) throws Exception; }

    /** Maintains a real SecurityContext and CSRF session, including fixation rotation and logout. */
    private class Client {
        private MockHttpSession session;
        private String csrf;
        private final String source = "198.51.100." + COUNTER.incrementAndGet();
        MvcResult get(String path) throws Exception { return raw("GET", path, null, false); }
        MvcResult login(String identifier, String password) throws Exception {
            return send("POST", "/api/auth/login", Map.of("identifier", identifier, "password", password));
        }
        MvcResult send(String method, String path, Object data) throws Exception {
            MvcResult token = get("/api/auth/csrf"); csrf = body(token).path("data").path("token").asText();
            return raw(method, path, data, true);
        }
        MvcResult raw(String method, String path, Object data, boolean withCsrf) throws Exception {
            if (session != null && session.isInvalid()) session = null;
            var request = MockMvcRequestBuilders.request(HttpMethod.valueOf(method), path)
                    .with(servlet -> { servlet.setRemoteAddr(source); return servlet; })
                    .header("X-Forwarded-For", "203.0.113." + COUNTER.incrementAndGet());
            if (session != null) request.session(session);
            if (withCsrf) request.header("X-XSRF-TOKEN", csrf);
            if (data != null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(data));
            MvcResult result = mvc.perform(request).andReturn();
            session = (MockHttpSession) result.getRequest().getSession(false);
            return result;
        }
    }
}
