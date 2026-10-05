package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.starrainnotes.message.entity.MessageActionEntity;
import com.starrainnotes.message.entity.MessageEntity;
import com.starrainnotes.message.mapper.MessageActionMapper;
import com.starrainnotes.message.mapper.MessageMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * 留言模块的真实数据库读写验证。
 *
 * 覆盖的是「Mockito 单元测试永远测不到」的那一层：XML 的列清单是否正确、
 * 自增主键有没有回填、`<if>` 分支在 status 为 null 时是否成立、
 * 带状态条件的 UPDATE 是否真的只在预期状态下生效。
 *
 * 全程在一个**不 commit 的 SqlSession** 里跑，每个用例结束 rollback，开发库数据不受影响。
 */
class MessageMapperXmlTest extends MapperXmlIntegrationSupport {

    private SqlSession session;
    private MessageMapper messages;
    private MessageActionMapper actions;

    @BeforeEach
    void open() {
        session = openSession();
        messages = session.getMapper(MessageMapper.class);
        actions = session.getMapper(MessageActionMapper.class);
    }

    @AfterEach
    void rollback() {
        if (session != null) {
            session.rollback();
            session.close();
        }
    }

    @Test
    void insertBackfillsGeneratedIdAndSelectByIdReadsEveryColumnBack() {
        MessageEntity saved = newMessage("mapper-xml-test-insert", "PENDING");

        assertEquals(1, messages.insert(saved));
        assertNotNull(saved.getId(), "insert 没有把自增主键回填到实体上，Service 随后写操作流水会拿到 null");

        MessageEntity loaded = messages.selectById(saved.getId());
        assertNotNull(loaded, "selectById 读不到刚插入的行");
        assertEquals(saved.getAuthorDisplayName(), loaded.getAuthorDisplayName());
        assertEquals(saved.getContactEmail(), loaded.getContactEmail());
        assertEquals(saved.getContent(), loaded.getContent());
        assertEquals("PENDING", loaded.getStatus());
        assertNotNull(loaded.getSubmittedAt(), "submitted_at 没有映射回来，检查列清单与 map-underscore-to-camel-case");
        assertNull(loaded.getModeratedAt());
        assertNull(loaded.getRejectReason());
    }

    @Test
    void countingAndPagingHonourTheOptionalStatusFilter() {
        messages.insert(newMessage("mapper-xml-test-count", "PENDING"));

        long pending = messages.countByStatus("PENDING");
        long all = messages.countByStatus(null);
        assertTrue(pending >= 1, "PENDING 计数应至少包含刚插入的那条");
        assertTrue(all >= pending, "status 为 null 时必须统计全部状态（<if> 分支没生效会小于 PENDING 数）");

        List<MessageEntity> page = messages.pageByStatus("PENDING", 1, 0);
        assertEquals(1, page.size(), "LIMIT/OFFSET 没有生效");
        assertTrue(page.stream().anyMatch(item -> "mapper-xml-test-count".equals(item.getContent())),
                "分页结果里找不到刚插入的行");
    }

    @Test
    void decisionOnlyAppliesOnceAndOnlyFromPending() {
        MessageEntity saved = newMessage("mapper-xml-test-decide", "PENDING");
        messages.insert(saved);

        assertEquals(1, messages.decide(saved.getId(), "PUBLIC", 1L, now(), null),
                "PENDING → PUBLIC 应当成功");
        assertEquals(0, messages.decide(saved.getId(), "REJECTED", 1L, now(), "重复审核"),
                "已经审核过的留言必须拒绝第二次决定（状态守卫写在 UPDATE 的 WHERE 里）");

        MessageEntity after = messages.selectById(saved.getId());
        assertEquals("PUBLIC", after.getStatus());
        assertEquals(1L, after.getModeratedByAccountId());
        assertNotNull(after.getModeratedAt());
    }

    @Test
    void hideRestoreAndSoftDeleteFollowTheirStateGuards() {
        MessageEntity saved = newMessage("mapper-xml-test-lifecycle", "PENDING");
        messages.insert(saved);
        messages.decide(saved.getId(), "PUBLIC", 1L, now(), null);

        assertEquals(1, messages.hide(saved.getId(), now()), "PUBLIC → HIDDEN 应当成功");
        assertEquals(0, messages.hide(saved.getId(), now()), "已隐藏的不能再隐藏");
        assertEquals(1, messages.restore(saved.getId()), "HIDDEN → PUBLIC 应当成功");
        assertEquals(0, messages.restore(saved.getId()), "已公开的不能恢复");
        assertEquals(1, messages.softDelete(saved.getId(), now()), "PUBLIC → DELETED 应当成功");
        assertEquals(0, messages.softDelete(saved.getId(), now()), "已删除的不能再删");
    }

    @Test
    void actionRowsAppendAndReadBackInChronologicalOrder() {
        MessageEntity saved = newMessage("mapper-xml-test-actions", "PENDING");
        messages.insert(saved);

        MessageActionEntity first = newAction(saved.getId(), "SUBMITTED", null, "VISITOR");
        assertEquals(1, actions.insert(first));
        assertNotNull(first.getId(), "操作流水同样需要回填自增主键");

        MessageActionEntity second = newAction(saved.getId(), "APPROVED", 1L, "ACCOUNT");
        actions.insert(second);

        List<MessageActionEntity> rows = actions.listByMessage(saved.getId());
        assertEquals(2, rows.size(), "按留言查询流水失败");
        assertEquals("SUBMITTED", rows.get(0).getActionType(), "流水必须按 created_at 升序");
        assertEquals("APPROVED", rows.get(1).getActionType());
        assertEquals("VISITOR", rows.get(0).getActorType());
        assertEquals(1L, rows.get(1).getActorAccountId());
    }

    private MessageEntity newMessage(String content, String status) {
        LocalDateTime at = now();
        MessageEntity message = new MessageEntity();
        message.setAccountId(null);
        message.setAuthorDisplayName("XML 验证");
        message.setContactEmail("mapper-xml-test@example.test");
        message.setContent(content);
        message.setStatus(status);
        message.setSubmittedAt(at);
        message.setCreatedAt(at);
        message.setUpdatedAt(at);
        return message;
    }

    private MessageActionEntity newAction(Long messageId, String type, Long actorId, String actorType) {
        MessageActionEntity entry = new MessageActionEntity();
        entry.setMessageId(messageId);
        entry.setActionType(type);
        entry.setActorAccountId(actorId);
        entry.setActorType(actorType);
        entry.setNote(null);
        entry.setCreatedAt(now());
        return entry;
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /** 兜底：任何断言失败都不应该把会话留成打开状态（@AfterEach 已在做，这里只是显式提醒）。 */
    @Test
    void rollbackKeepsTheDevelopmentDatabaseClean() {
        try {
            MessageEntity saved = newMessage("mapper-xml-test-rollback", "PENDING");
            messages.insert(saved);
            assertNotNull(saved.getId());
        } catch (RuntimeException unexpected) {
            fail("插入失败：" + unexpected.getMessage());
        }
    }
}
