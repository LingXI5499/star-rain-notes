package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.*;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialCategoryMapper;
import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TutorialAdminPagingIntegrationTest extends MapperXmlIntegrationSupport {
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
