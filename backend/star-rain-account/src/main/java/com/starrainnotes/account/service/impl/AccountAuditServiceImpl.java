/*
 * AccountAuditServiceImpl：账号模块的业务实现文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：AccountAuditEntity、AccountMapper、AccountAuditService。
 * 阅读入口：success（success处理入口）；failed（失败处理入口）；failedIndependently（失败独立执行处理入口）。
 */

package com.starrainnotes.account.service.impl;

import com.starrainnotes.account.entity.AccountAuditEntity;
import com.starrainnotes.account.mapper.AccountMapper;
import com.starrainnotes.account.service.AccountAuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 账号审计业务服务实现，属于账号模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
public class AccountAuditServiceImpl implements AccountAuditService {

    /**
     * 数据映射。
     * 协作类型：<code>AccountMapper</code>。
     */
    private final AccountMapper mapper;

    /**
     * 构造AccountAuditServiceImpl，接收本类使用的依赖或初始化参数。
     *
     * @param mapper 数据映射；类型为 <code>AccountMapper</code>
     */
    public AccountAuditServiceImpl(AccountMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * success处理入口。
     * 关键处理步骤调用 <code>writeAudit</code>。
     *
     * @param actor 当前操作对应的身份对象；类型为 <code>Long</code>
     * @param target 目标；类型为 <code>Long</code>
     * @param action 动作；类型为 <code>String</code>
     */
    @Override
    public void success(Long actor, Long target, String action) {
        writeAudit(actor, target, action, "SUCCESS");
    }

    /**
     * 失败处理入口。
     * 关键处理步骤调用 <code>writeAudit</code>。
     *
     * @param actor 当前操作对应的身份对象；类型为 <code>Long</code>
     * @param target 目标；类型为 <code>Long</code>
     * @param action 动作；类型为 <code>String</code>
     */
    @Override
    public void failed(Long actor, Long target, String action) {
        writeAudit(actor, target, action, "FAILED");
    }

    /*
     * REQUIRES_NEW：挂起调用方事务，另起一个事务写完就提交。
     * 用于「审计描述的是调用方这次失败」的场景——否则它会跟着回滚一起消失。
     */
    /**
     * 失败独立执行处理入口。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>writeAudit</code>。
     *
     * @param actor 当前操作对应的身份对象；类型为 <code>Long</code>
     * @param target 目标；类型为 <code>Long</code>
     * @param action 动作；类型为 <code>String</code>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failedIndependently(Long actor, Long target, String action) {
        writeAudit(actor, target, action, "FAILED");
    }

    /**
     * 写入审计。
     * 主要协作调用：<code>mapper.insertAudit</code>。
     *
     * @param actor 当前操作对应的身份对象；类型为 <code>Long</code>
     * @param target 目标；类型为 <code>Long</code>
     * @param action 动作；类型为 <code>String</code>
     * @param result 结果；类型为 <code>String</code>
     */
    private void writeAudit(Long actor, Long target, String action, String result) {
        AccountAuditEntity row = new AccountAuditEntity();
        row.setActorAccountId(actor);
        row.setTargetAccountId(target);
        row.setActionCode(action);
        row.setResult(result);
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            row.setIpAddress(truncate(request.getRemoteAddr(), 45));
            row.setUserAgent(truncate(request.getHeader("User-Agent"), 500));
        }
        mapper.insertAudit(row);
    }

    /**
     * truncate处理入口。
     * 关键处理步骤调用 <code>value.substring</code>、<code>Math.min</code>、<code>value.length</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @param maxLength 最大Length；类型为 <code>int</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private static String truncate(String value, int maxLength) {
        return value == null ? null : value.substring(0, Math.min(value.length(), maxLength));
    }
}
