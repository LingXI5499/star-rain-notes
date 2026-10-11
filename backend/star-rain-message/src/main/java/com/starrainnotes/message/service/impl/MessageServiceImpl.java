/*
 * MessageServiceImpl：留言模块的业务实现文件。
 * 业务范围：留言提交、审核展示和后台管理。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：AccountReferenceApi、CurrentActorApi、ApiException、PageResult、MessageSummaryApi、MessageSubmitDTO、MessageActionEntity。
 * 阅读入口：submit（提交留言）；publicMessages（公开Messages处理入口）；adminMessages（后台Messages处理入口）；actions（actions处理入口）。
 */

package com.starrainnotes.message.service.impl;

import com.starrainnotes.account.api.AccountReferenceApi;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.message.api.MessageSummaryApi;
import com.starrainnotes.message.dto.MessageSubmitDTO;
import com.starrainnotes.message.entity.MessageActionEntity;
import com.starrainnotes.message.entity.MessageEntity;
import com.starrainnotes.message.mapper.MessageActionMapper;
import com.starrainnotes.message.mapper.MessageMapper;
import com.starrainnotes.message.service.MessageService;
import com.starrainnotes.message.service.MessageSubmissionLimiter;
import com.starrainnotes.message.vo.AdminMessageVO;
import com.starrainnotes.message.vo.MessageActionVO;
import com.starrainnotes.message.vo.PublicMessageVO;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 留言业务服务实现，属于留言模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService, MessageSummaryApi {
    /**
     * 邮箱。
     */
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    /**
     * STATUSES。
     */
    private static final Set<String> STATUSES = Set.of("PENDING", "PUBLIC", "REJECTED", "HIDDEN", "DELETED");
    /**
     * messages。
     * 协作类型：<code>MessageMapper</code>。
     */
    private final MessageMapper messages;
    /**
     * actions。
     * 协作类型：<code>MessageActionMapper</code>。
     */
    private final MessageActionMapper actions;
    /**
     * 当前操作者。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi currentActor;
    /**
     * 账号集合。
     * 协作类型：<code>AccountReferenceApi</code>。
     */
    private final AccountReferenceApi accounts;
    /**
     * limiter。
     */
    private final MessageSubmissionLimiter limiter;

    /**
     * 提交留言。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>limiter.check</code>、<code>currentActor.currentOptional</code>、<code>accounts.displayName</code>、<code>EMAIL.matcher</code>、<code>messages.insert</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>MessageSubmitDTO</code>
     * @param remoteAddress 远端地址；类型为 <code>String</code>
     * @return 类型为 <code>Long</code> 的处理结果
     */
    @Override
    @Transactional
    public Long submit(MessageSubmitDTO request, String remoteAddress) {
        limiter.check(remoteAddress);
        if (request == null) {
            throw invalid("MESSAGE_CONTENT_INVALID", "请填写留言内容");
        }
        String content = trim(request.getContent());
        if (content.isEmpty() || content.length() > 3000) {
            throw invalid("MESSAGE_CONTENT_INVALID", "留言内容需在 1 到 3000 字之间");
        }
        Long accountId = currentActor.currentOptional().map(CurrentActorApi.CurrentActor::getAccountId).orElse(null);
        String name = accountId == null ? trim(request.getAuthorDisplayName()) : trim(accounts.displayName(accountId));
        if (name.isEmpty() || name.length() > 100) {
            throw invalid("MESSAGE_AUTHOR_INVALID", "署名需在 1 到 100 字之间");
        }
        String email = trim(request.getContactEmail());
        if (!email.isEmpty() && (email.length() > 128 || !EMAIL.matcher(email).matches())) {
            throw invalid("MESSAGE_CONTACT_INVALID", "联系邮箱格式不正确");
        }
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        MessageEntity message = new MessageEntity();
        message.setAccountId(accountId);
        message.setAuthorDisplayName(name);
        message.setContactEmail(email.isEmpty() ? null : email);
        message.setContent(content);
        message.setStatus("PENDING");
        message.setSubmittedAt(now);
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        messages.insert(message);
        action(message.getId(), "SUBMITTED", accountId, null);
        return message.getId();
    }

    /**
     * 公开Messages处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>messages.countByStatus</code>、<code>messages.pageByStatus</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;PublicMessageVO&gt;</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<PublicMessageVO> publicMessages(int page, int pageSize) {
        validatePage(page, pageSize);
        long total = messages.countByStatus("PUBLIC");
        List<PublicMessageVO> items = messages
                .pageByStatus("PUBLIC", pageSize, (long) (page - 1) * pageSize)
                .stream().map(this::publicView).toList();
        return new PageResult<>(items, total, page, pageSize);
    }

    /**
     * 后台Messages处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>STATUSES.contains</code>、<code>messages.countByStatus</code>、<code>messages.pageByStatus</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @return 类型为 <code>PageResult&lt;AdminMessageVO&gt;</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminMessageVO> adminMessages(int page, int pageSize, String status) {
        validatePage(page, pageSize);
        if (status != null && !STATUSES.contains(status)) {
            throw invalid("MESSAGE_STATE_INVALID", "留言状态无效");
        }
        // status 为 null 即「全部状态」，与原先 lambdaQuery 的 eq(condition, ...) 语义一致
        long total = messages.countByStatus(status);
        List<AdminMessageVO> items = messages
                .pageByStatus(status, pageSize, (long) (page - 1) * pageSize)
                .stream().map(this::adminView).toList();
        return new PageResult<>(items, total, page, pageSize);
    }

    /**
     * actions处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>actions.listByMessage</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    @Transactional(readOnly = true)
    public List<MessageActionVO> actions(long id) {
        require(id);
        return actions.listByMessage(id)
                .stream().map(item -> new MessageActionVO(item.getActionType(), item.getActorType(),
                        item.getCreatedAt(), item.getNote())).toList();
    }

    /**
     * 批准留言。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>decide</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>AdminMessageVO</code> 的处理结果
     */
    @Override
    @Transactional
    public AdminMessageVO approve(long id) {
        return decide(id, "PUBLIC", null, "APPROVED");
    }

    /**
     * 拒绝留言。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>trim</code>、<code>note.isEmpty</code>、<code>note.length</code>、<code>invalid</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param reason 原因；类型为 <code>String</code>
     * @return 类型为 <code>AdminMessageVO</code> 的处理结果
     */
    @Override
    @Transactional
    public AdminMessageVO reject(long id, String reason) {
        String note = trim(reason);
        if (note.isEmpty() || note.length() > 1000) {
            throw invalid("MESSAGE_REJECT_REASON_REQUIRED", "请填写不超过 1000 字的拒绝原因");
        }
        return decide(id, "REJECTED", note, "REJECTED");
    }

    /**
     * 作出决定留言。
     * 主要协作调用：<code>currentActor.current</code>、<code>messages.decide</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param target 目标；类型为 <code>String</code>
     * @param reason 原因；类型为 <code>String</code>
     * @param actionType 动作类型；类型为 <code>String</code>
     * @return 类型为 <code>AdminMessageVO</code> 的处理结果
     */
    private AdminMessageVO decide(long id, String target, String reason, String actionType) {
        long actorId = currentActor.current().getAccountId();
        if (messages.decide(id, target, actorId, LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS), reason) != 1) {
            stateError(id, true);
        }
        action(id, actionType, actorId, reason);
        return adminView(require(id));
    }

    /**
     * hide处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>currentActor.current</code>、<code>messages.hide</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>AdminMessageVO</code> 的处理结果
     */
    @Override
    @Transactional
    public AdminMessageVO hide(long id) {
        long actorId = currentActor.current().getAccountId();
        if (messages.hide(id, LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)) != 1) {
            stateError(id, false);
        }
        action(id, "HIDDEN", actorId, null);
        return adminView(require(id));
    }

    /**
     * 恢复留言。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>currentActor.current</code>、<code>messages.restore</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>AdminMessageVO</code> 的处理结果
     */
    @Override
    @Transactional
    public AdminMessageVO restore(long id) {
        long actorId = currentActor.current().getAccountId();
        if (messages.restore(id) != 1) {
            stateError(id, false);
        }
        action(id, "RESTORED", actorId, null);
        return adminView(require(id));
    }

    /**
     * 删除留言。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>currentActor.current</code>、<code>messages.softDelete</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     */
    @Override
    @Transactional
    public void delete(long id) {
        long actorId = currentActor.current().getAccountId();
        if (messages.softDelete(id, LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)) != 1) {
            stateError(id, false);
        }
        action(id, "DELETED", actorId, null);
    }

    /**
     * 待处理数量处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>messages.countByStatus</code>。
     * @return 类型为 <code>long</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public long pendingCount() {
        return messages.countByStatus("PENDING");
    }

    /**
     * 动作处理入口。
     * 主要协作调用：<code>actions.insert</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param type 类型；类型为 <code>String</code>
     * @param actorId 操作者标识；类型为 <code>Long</code>
     * @param note note；类型为 <code>String</code>
     */
    private void action(Long id, String type, Long actorId, String note) {
        MessageActionEntity entry = new MessageActionEntity();
        entry.setMessageId(id);
        entry.setActionType(type);
        entry.setActorAccountId(actorId);
        entry.setActorType(actorId == null ? "VISITOR" : "ACCOUNT");
        entry.setNote(note);
        entry.setCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        actions.insert(entry);
    }

    /**
     * 检查所需留言。
     * 主要协作调用：<code>messages.selectById</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>MessageEntity</code> 的处理结果
     */
    private MessageEntity require(long id) {
        MessageEntity message = id <= 0 ? null : messages.selectById(id);
        if (message == null) {
            throw new ApiException("MESSAGE_NOT_FOUND", "留言不存在", 404);
        }
        return message;
    }

    /**
     * 状态错误处理入口。
     * 关键处理步骤调用 <code>require</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param moderated moderated；类型为 <code>boolean</code>
     */
    private void stateError(long id, boolean moderated) {
        require(id);
        throw new ApiException(moderated ? "MESSAGE_ALREADY_MODERATED" : "MESSAGE_STATE_INVALID",
                moderated ? "这条留言已由其他管理员审核" : "当前状态不能执行此操作", 409);
    }

    /**
     * 公开View处理入口。
     * 关键处理步骤调用 <code>message.getId</code>、<code>message.getAuthorDisplayName</code>、<code>message.getContent</code>、<code>message.getSubmittedAt</code>。
     *
     * @param message 留言；类型为 <code>MessageEntity</code>
     * @return 类型为 <code>PublicMessageVO</code> 的处理结果
     */
    private PublicMessageVO publicView(MessageEntity message) {
        return new PublicMessageVO(message.getId(), message.getAuthorDisplayName(),
                message.getContent(), message.getSubmittedAt());
    }

    /**
     * 后台View处理入口。
     * 关键处理步骤调用 <code>message.getId</code>、<code>message.getAuthorDisplayName</code>、<code>message.getContactEmail</code>、<code>message.getContent</code>。
     *
     * @param message 留言；类型为 <code>MessageEntity</code>
     * @return 类型为 <code>AdminMessageVO</code> 的处理结果
     */
    private AdminMessageVO adminView(MessageEntity message) {
        return new AdminMessageVO(message.getId(), message.getAuthorDisplayName(),
                message.getContactEmail(), message.getContent(), message.getStatus(),
                message.getSubmittedAt(), message.getModeratedAt(), message.getRejectReason());
    }

    /**
     * 校验分页。
     * 关键处理步骤调用 <code>invalid</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     */
    private void validatePage(int page, int pageSize) {
        if (page < 1 || page > 100_000 || pageSize < 1 || pageSize > 100) {
            throw invalid("MESSAGE_CONTENT_INVALID", "分页参数无效");
        }
    }

    /**
     * 去除首尾空白处理入口。
     * 关键处理步骤调用 <code>value.trim</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 无效处理入口。
     *
     * @param code 代码；类型为 <code>String</code>
     * @param message 留言；类型为 <code>String</code>
     * @return 类型为 <code>ApiException</code> 的处理结果
     */
    private ApiException invalid(String code, String message) {
        return new ApiException(code, message, 400);
    }
}
