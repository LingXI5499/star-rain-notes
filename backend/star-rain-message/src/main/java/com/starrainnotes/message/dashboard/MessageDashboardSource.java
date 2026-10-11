package com.starrainnotes.message.dashboard;

import com.starrainnotes.message.api.MessageSummaryApi;

import com.starrainnotes.common.dashboard.api.DashboardSource;
import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageDashboardSource implements DashboardSource {
    private final MessageSummaryApi messages;

    @Override public String moduleCode() { return "MESSAGE"; }

    @Override public DashboardModuleData load() {
        return new DashboardModuleData(Map.of("pending", messages.pendingCount()), List.of());
    }
}
