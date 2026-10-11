package com.starrainnotes.account.dashboard;

import com.starrainnotes.account.service.AdminAccountService;
import com.starrainnotes.common.dashboard.api.DashboardSource;
import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountDashboardSource implements DashboardSource {
    private final AdminAccountService accounts;

    @Override public String moduleCode() { return "ACCOUNT"; }

    @Override public DashboardModuleData load() {
        return new DashboardModuleData(Map.of(
                "total", accounts.accounts(1, 1, null, null).getTotal(),
                "active", accounts.accounts(1, 1, null, "ACTIVE").getTotal()), List.of());
    }
}
