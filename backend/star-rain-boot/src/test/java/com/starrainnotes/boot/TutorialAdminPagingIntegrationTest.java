/*
 * TutorialAdminPagingIntegrationTest：应用启动模块的测试文件。
 * 业务范围：多模块装配、入口路由、安全配置和启动初始化。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：TutorialMapper、TutorialCategoryMapper。
 * 阅读入口：filtersAndCountsInSqlAndPreservesCompleteSortingProjection（筛选及数量集合内部Sql及PreservesCompleteSortingProjection处理入口）。
 */

package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.*;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialCategoryMapper;
import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * 教程后台PagingIntegration测试，属于应用启动模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class TutorialAdminPagingIntegrationTest extends MapperXmlIntegrationSupport {
    /**
     * 筛选及数量集合内部Sql及PreservesCompleteSortingProjection处理入口。
     * 关键处理步骤调用 <code>openSession</code>、<code>session.getConnection</code>、<code>UUID.randomUUID</code>、<code>connection.prepareStatement</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test void filtersAndCountsInSqlAndPreservesCompleteSortingProjection() throws Exception {
        try (var session = openSession()) {
            Connection connection = session.getConnection();
            String prefix = "paging-" + UUID.randomUUID();
            long category;
            try (var insert = connection.prepareStatement("INSERT INTO sr_tutorial_category(name,slug) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS)) {
                insert.setString(1, "Paging probe"); insert.setString(2, prefix); insert.executeUpdate();
                try (var keys = insert.getGeneratedKeys()) { assertTrue(keys.next()); category = keys.getLong(1); }
            }
            try (var insert = connection.prepareStatement("INSERT INTO sr_tutorial(category_id,slug,title,sort_order,created_by_account_id,updated_by_account_id) VALUES (?,?,?,?,1,1)")) {
                for (int i = 0; i < 1000; i++) {
                    insert.setLong(1, category); insert.setString(2, prefix + "-" + i);
                    insert.setString(3, (i % 2 == 0 ? "Java" : "Other") + " " + i); insert.setInt(4, i); insert.addBatch();
                }
                insert.executeBatch();
            }
            var mapper = session.getMapper(TutorialMapper.class);
            assertEquals(1000, mapper.adminCount(category, null));
            assertEquals(500, mapper.adminCount(category, "Java"));
            var second = mapper.adminPage(category, "Java", 20L, 20);
            assertEquals(20, second.size()); assertEquals("Java 40", second.getFirst().getTitle());
            assertTrue(second.stream().allMatch(item -> item.getCategoryId() == category && item.getTitle().startsWith("Java")));
            assertEquals(1000, mapper.orderItems(category).size());
            assertEquals(0, mapper.adminCount(category, "%"));
            assertEquals(0, mapper.adminCount(Long.MAX_VALUE, null));
            var categories = session.getMapper(TutorialCategoryMapper.class).listOrdered();
            var aggregate = categories.stream().filter(item -> item.getId() == category).findFirst().orElseThrow();
            assertEquals(1000L, aggregate.getTutorialCount()); assertEquals(0L, aggregate.getChapterCount());
            session.rollback();
        }
    }
}
