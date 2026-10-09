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

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService, MessageSummaryApi {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Set<String> STATUSES = Set.of("PENDING", "PUBLIC", "REJECTED", "HIDDEN", "DELETED");
    private final MessageMapper messages;
    private final MessageActionMapper actions;
    private final CurrentActorApi currentActor;
    private final AccountReferenceApi accounts;
    private final MessageSubmissionLimiter limiter;

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

    @Override
    @Transactional(readOnly = true)
    public List<MessageActionVO> actions(long id) {
        require(id);
        return actions.listByMessage(id)
                .stream().map(item -> new MessageActionVO(item.getActionType(), item.getActorType(),
                        item.getCreatedAt(), item.getNote())).toList();
    }

    @Override
    @Transactional
    public AdminMessageVO approve(long id) {
        return decide(id, "PUBLIC", null, "APPROVED");
    }

    @Override
    @Transactional
    public AdminMessageVO reject(long id, String reason) {
        String note = trim(reason);
        if (note.isEmpty() || note.length() > 1000) {
            throw invalid("MESSAGE_REJECT_REASON_REQUIRED", "请填写不超过 1000 字的拒绝原因");
        }
        return decide(id, "REJECTED", note, "REJECTED");
    }

    private AdminMessageVO decide(long id, String target, String reason, String actionType) {
        long actorId = currentActor.current().getAccountId();
        if (messages.decide(id, target, actorId, LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS), reason) != 1) {
            stateError(id, true);
        }
        action(id, actionType, actorId, reason);
        return adminView(require(id));
    }

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

    @Override
    @Transactional
    public void delete(long id) {
        long actorId = currentActor.current().getAccountId();
        if (messages.softDelete(id, LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)) != 1) {
            stateError(id, false);
        }
        action(id, "DELETED", actorId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public long pendingCount() {
        return messages.countByStatus("PENDING");
    }

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

    private MessageEntity require(long id) {
        MessageEntity message = id <= 0 ? null : messages.selectById(id);
        if (message == null) {
            throw new ApiException("MESSAGE_NOT_FOUND", "留言不存在", 404);
        }
        return message;
    }

    private void stateError(long id, boolean moderated) {
        require(id);
        throw new ApiException(moderated ? "MESSAGE_ALREADY_MODERATED" : "MESSAGE_STATE_INVALID",
                moderated ? "这条留言已由其他管理员审核" : "当前状态不能执行此操作", 409);
    }

    private PublicMessageVO publicView(MessageEntity message) {
        return new PublicMessageVO(message.getId(), message.getAuthorDisplayName(),
                message.getContent(), message.getSubmittedAt());
    }

    private AdminMessageVO adminView(MessageEntity message) {
        return new AdminMessageVO(message.getId(), message.getAuthorDisplayName(),
                message.getContactEmail(), message.getContent(), message.getStatus(),
                message.getSubmittedAt(), message.getModeratedAt(), message.getRejectReason());
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || page > 100_000 || pageSize < 1 || pageSize > 100) {
            throw invalid("MESSAGE_CONTENT_INVALID", "分页参数无效");
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private ApiException invalid(String code, String message) {
        return new ApiException(code, message, 400);
    }
}
