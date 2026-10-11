package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.*;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BlogListProjectionIntegrationTest extends MapperXmlIntegrationSupport {
    @Test void listsExcludeLargeBodyWhilePublicDetailsKeepIt() throws Exception {
        try (var session = openSession()) {
            String slug = "projection-" + UUID.randomUUID();
            String body = "# Semantic content\n" + "large body ".repeat(10_000);
            try (var insert = session.getConnection().prepareStatement("INSERT INTO sr_blog_post(slug,title,summary,body_markdown,status,published_at,created_by_account_id,updated_by_account_id) VALUES (?,?,'Readable summary',?,'PUBLISHED',NOW(),1,1)")) {
                insert.setString(1, slug); insert.setString(2, slug); insert.setString(3, body); insert.executeUpdate();
            }
            var mapper = session.getMapper(BlogPostMapper.class);
            var listed = mapper.adminPage(slug, null, null, null, 0, 20);
            assertEquals(1, listed.size());
            assertNull(listed.getFirst().getBodyMarkdown());
            assertEquals("Readable summary", listed.getFirst().getSummary());
            assertEquals(body, mapper.publishedPostBySlug(slug).getBodyMarkdown());
            assertTrue(mapper.latestPublished(20).stream().allMatch(post -> post.getBodyMarkdown() == null));
            session.rollback();
        }
    }
}
