package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.seo.api.dto.SeoPageSnapshot;
import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.search.api.dto.SearchableDocument;
import com.starrainnotes.search.mapper.SearchDocumentMapper;
import com.starrainnotes.tutorial.learning.mapper.LearningProgressMapper;
import com.starrainnotes.account.delivery.MailOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionTemplate;

class AuditUpgradeMapperTest extends MapperXmlIntegrationSupport {
    @Test void failedProjectionRemovalCannotExposeWithdrawnBlogHtmlMetadataSitemapOrSearch() {
        try (var session = openSession()) {
            var blogs = session.getMapper(BlogPostMapper.class);
            var pages = session.getMapper(SeoPageMapper.class);
            var search = session.getMapper(SearchDocumentMapper.class);
            String token = "audit" + UUID.randomUUID().toString().replace("-", "");
            var post = new BlogPostEntity(); post.setSlug(token); post.setTitle(token); post.setSummary(token);
            post.setBodyMarkdown("private正文 " + token); post.setStatus("PUBLISHED");
            post.setCreatedByAccountId(1L); post.setUpdatedByAccountId(1L); blogs.insertPost(post);
            String route = "/blog/posts/" + token;
            var page = new SeoPageSnapshot(); page.setRoutePath(route); page.setContentType("BLOG"); page.setContentId(post.getId());
            page.setCanonicalUrl("https://example.org" + route); page.setTitle(token); page.setHtmlSnapshot("private正文");
            page.setDescription(token); page.setRobotsDirective("index,follow");
            pages.upsert(page);
            var document = new SearchableDocument(); document.setDocumentKey("BLOG:" + post.getId()); document.setContentType("BLOG");
            document.setContentId(post.getId()); document.setTitle(token); document.setSummary(token); document.setSearchableText(token); document.setRoutePath(route);
            document.setSourceUpdatedAt(java.time.LocalDateTime.now()); search.upsert(document);
            assertNotNull(pages.active(route));
            long visibleBefore = search.count(token, List.of("BLOG"));
            assertTrue(search.search(token, List.of("BLOG"), 0, 100).stream().anyMatch(hit -> post.getId().toString().equals(hit.getContentId())));
            blogs.withdrawPost(post.getId(), java.time.LocalDateTime.now(), 1L);
            // Deliberately leave both projections ACTIVE, simulating failed consumers.
            assertNull(pages.active(route)); assertNull(pages.activeMeta(route));
            assertTrue(pages.activePages().stream().noneMatch(p -> route.equals(p.getRoutePath())));
            assertEquals(visibleBefore - 1, search.count(token, List.of("BLOG")));
            assertTrue(search.suggestions(token, 10).isEmpty());
            session.rollback();
        }
    }

    @Test void repeatedAndOutOfOrderReadingEventsDoNotDoubleCountOrRegressPosition() {
        try (var session = openSession()) {
            var progress = session.getMapper(LearningProgressMapper.class);
            Long chapter = 800000000L + Math.abs(System.nanoTime() % 1000000);
            String event = UUID.randomUUID().toString();
            progress.ensureRow(1L, 1L, 1L, chapter);
            assertEquals(1, progress.acceptEvent(1L, event));
            assertEquals(1, progress.applyReading(1L, chapter, "new", new BigDecimal("0.8"), 10, 0));
            assertEquals(0, progress.acceptEvent(1L, event));
            assertEquals(0, progress.applyReading(1L, chapter, "old", new BigDecimal("0.1"), 10, 0));
            var row = progress.selectByAccountIdAndChapterId(1L, chapter);
            assertEquals("new", row.getScrollAnchor()); assertEquals(10L, row.getStudySecondsTotal()); assertEquals(1L, row.getRowVersion());
            assertEquals(1, progress.applyReading(1L, chapter, "later", new BigDecimal("0.9"), 10, 1));
            assertTrue(progress.selectByAccountIdAndChapterId(1L, chapter).getStudySecondsTotal() <= 11, "overlapping tabs cannot each claim ten seconds");
            session.rollback();
        }
    }

    @Test void rollbackRemovesEncryptedMailTaskAndSmtpNeverRunsInTheBusinessTransaction() {
        try (var session = openSession()) {
            var source = session.getConfiguration().getEnvironment().getDataSource();
            var jdbc = new JdbcTemplate(source); var smtp = mock(JavaMailSender.class);
            var outbox = new MailOutbox(jdbc, new ObjectMapper(), smtp, Base64.getEncoder().encodeToString(new byte[32]), true, session.getMapper(com.starrainnotes.account.mapper.AccountMapper.class), new DataSourceTransactionManager(jdbc.getDataSource()));
            String[] id = new String[1];
            new TransactionTemplate(new DataSourceTransactionManager(source)).executeWithoutResult(status -> {
                id[0] = outbox.enqueue("sender@example.test", "recipient@example.test", "验证码", "one-time-secret-123456").substring(7);
                String encrypted = jdbc.queryForObject("SELECT payload_cipher FROM sr_mail_outbox WHERE id=?", String.class, id[0]);
                assertFalse(encrypted.contains("123456")); verifyNoInteractions(smtp);
                status.setRollbackOnly();
            });
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM sr_mail_outbox WHERE id=?", Integer.class, id[0]));
            verifyNoInteractions(smtp);
        }
    }

    @Test void committedMailRetriesAndRemovesPayloadAfterSuccessfulDelivery() throws Exception {
        try (var session = openSession()) {
            var jdbc = new JdbcTemplate(session.getConfiguration().getEnvironment().getDataSource());
            var smtp = mock(JavaMailSender.class);
            when(smtp.createMimeMessage()).thenAnswer(call -> new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new java.util.Properties())));
            doThrow(new org.springframework.mail.MailSendException("injected test failure")).doNothing().when(smtp).send(any(jakarta.mail.internet.MimeMessage.class));
            var outbox = new MailOutbox(jdbc, new ObjectMapper(), smtp, Base64.getEncoder().encodeToString(new byte[32]), true, session.getMapper(com.starrainnotes.account.mapper.AccountMapper.class), new DataSourceTransactionManager(jdbc.getDataSource()));
            var transaction = new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()));
            String id = transaction.execute(status -> outbox.enqueue("sender@example.test", "recipient@example.test", "验证码", "isolated secret").substring(7));
            try {
                outbox.deliver();
                assertEquals(1, jdbc.queryForObject("SELECT attempts FROM sr_mail_outbox WHERE id=?", Integer.class, id));
                outbox.deliver(); verify(smtp, times(1)).send(any(jakarta.mail.internet.MimeMessage.class));
                jdbc.update("UPDATE sr_mail_outbox SET next_attempt_at=UTC_TIMESTAMP(3) WHERE id=?", id);
                outbox.deliver(); verify(smtp, times(2)).send(any(jakarta.mail.internet.MimeMessage.class));
                assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM sr_mail_outbox WHERE id=?", Integer.class, id));
            } finally { jdbc.update("DELETE FROM sr_mail_outbox WHERE id=?", id); }
        }
    }
}
