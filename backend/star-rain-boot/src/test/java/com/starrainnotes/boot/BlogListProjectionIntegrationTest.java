/*
 * BlogListProjectionIntegrationTest：应用启动模块的测试文件。
 * 业务范围：多模块装配、入口路由、安全配置和启动初始化。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：BlogPostMapper。
 * 阅读入口：listsExcludeLargeBodyWhilePublicDetailsKeepIt（listsExcludeLarge正文While公开详情KeepIt处理入口）。
 */

package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.*;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * 博客ListProjectionIntegration测试，属于应用启动模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class BlogListProjectionIntegrationTest extends MapperXmlIntegrationSupport {
    /**
     * listsExcludeLarge正文While公开详情KeepIt处理入口。
     * 关键处理步骤调用 <code>openSession</code>、<code>UUID.randomUUID</code>、<code>session.getConnection</code>、<code>insert.setString</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
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
