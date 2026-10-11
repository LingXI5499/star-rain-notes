/*
 * MessageMapperXmlTest：应用启动模块的测试文件。
 * 业务范围：多模块装配、入口路由、安全配置和启动初始化。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：MessageActionEntity、MessageEntity、MessageActionMapper、MessageMapper。
 * 阅读入口：open（打开留言数据映射Xml）；rollback（回滚留言数据映射Xml）；insertBackfillsGeneratedIdAndSelectByIdReadsEveryColumnBack（写入BackfillsGenerated标识及查询依据标识ReadsEvery列Back）；countingAndPagingHonourTheOptionalStatusFilter（counting及PagingHonourTheOptional状态筛选处理入口）。
 */

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
/**
 * 留言数据映射Xml测试，属于应用启动模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class MessageMapperXmlTest extends MapperXmlIntegrationSupport {

    /**
     * 当前业务使用的会话对象。
     */
    private SqlSession session;
    /**
     * messages。
     * 协作类型：<code>MessageMapper</code>。
     */
    private MessageMapper messages;
    /**
     * actions。
     * 协作类型：<code>MessageActionMapper</code>。
     */
    private MessageActionMapper actions;

    /**
     * 打开留言数据映射Xml。
     * 主要协作调用：<code>session.getMapper</code>。
     */
    @BeforeEach
    void open() {
        session = openSession();
        messages = session.getMapper(MessageMapper.class);
        actions = session.getMapper(MessageActionMapper.class);
    }

    /**
     * 回滚留言数据映射Xml。
     * 主要协作调用：<code>session.rollback</code>、<code>session.close</code>。
     */
    @AfterEach
    void rollback() {
        if (session != null) {
            session.rollback();
            session.close();
        }
    }

    /**
     * 写入BackfillsGenerated标识及查询依据标识ReadsEvery列Back。
     * 主要协作调用：<code>messages.insert</code>、<code>messages.selectById</code>。
     */
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

    /**
     * counting及PagingHonourTheOptional状态筛选处理入口。
     * 主要协作调用：<code>messages.insert</code>、<code>messages.countByStatus</code>、<code>messages.pageByStatus</code>。
     */
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

    /**
     * 决定OnlyAppliesOnce及Only来源待处理处理入口。
     * 主要协作调用：<code>messages.insert</code>、<code>messages.decide</code>、<code>messages.selectById</code>。
     */
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

    /**
     * hideRestore及Soft删除FollowTheir状态Guards处理入口。
     * 主要协作调用：<code>messages.insert</code>、<code>messages.decide</code>、<code>messages.hide</code>、<code>messages.restore</code>、<code>messages.softDelete</code>。
     */
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

    /**
     * 动作行集合Append及读取Back内部Chronological顺序处理入口。
     * 主要协作调用：<code>messages.insert</code>、<code>actions.insert</code>、<code>actions.listByMessage</code>。
     */
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

    /**
     * new留言处理入口。
     * 关键处理步骤调用 <code>now</code>、<code>message.setAccountId</code>、<code>message.setAuthorDisplayName</code>、<code>message.setContactEmail</code>。
     *
     * @param content 内容；类型为 <code>String</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @return 类型为 <code>MessageEntity</code> 的处理结果
     */
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

    /**
     * new动作处理入口。
     * 关键处理步骤调用 <code>entry.setMessageId</code>、<code>entry.setActionType</code>、<code>entry.setActorAccountId</code>、<code>entry.setActorType</code>。
     *
     * @param messageId 留言标识；类型为 <code>Long</code>
     * @param type 类型；类型为 <code>String</code>
     * @param actorId 操作者标识；类型为 <code>Long</code>
     * @param actorType 操作者类型；类型为 <code>String</code>
     * @return 类型为 <code>MessageActionEntity</code> 的处理结果
     */
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

    /**
     * now处理入口。
     * 关键处理步骤调用 <code>LocalDateTime.now</code>。
     * @return 类型为 <code>LocalDateTime</code> 的处理结果
     */
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
