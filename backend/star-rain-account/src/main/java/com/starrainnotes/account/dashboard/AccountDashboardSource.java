/*
 * AccountDashboardSource：账号模块的模块支持文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：AdminAccountService、DashboardSource、DashboardModuleData。
 * 阅读入口：moduleCode（module代码处理入口）；load（加载账号概览来源）。
 */

package com.starrainnotes.account.dashboard;

import com.starrainnotes.account.service.AdminAccountService;
import com.starrainnotes.common.dashboard.api.DashboardSource;
import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 账号概览来源，属于账号模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class AccountDashboardSource implements DashboardSource {
    /**
     * 账号集合。
     * 协作类型：<code>AdminAccountService</code>。
     */
    private final AdminAccountService accounts;

    /**
     * module代码处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override public String moduleCode() { return "ACCOUNT"; }

    /**
     * 加载账号概览来源。
     * 主要协作调用：<code>accounts.accounts</code>。
     * @return 类型为 <code>DashboardModuleData</code> 的处理结果
     */
    @Override public DashboardModuleData load() {
        return new DashboardModuleData(Map.of(
                "total", accounts.accounts(1, 1, null, null).getTotal(),
                "active", accounts.accounts(1, 1, null, "ACTIVE").getTotal()), List.of());
    }
}
