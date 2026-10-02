package com.starrainnotes.review.handler.impl;

import com.starrainnotes.review.api.ReviewTargetView;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 演示用目标视图。
 *
 * 存在的唯一理由是：Tutorial / Blog 模块还没有实现，REV-003 的
 * 「目标模块按 revisionRef 返回不可变审核视图」这条链路如果没有任何实现，
 * 就无法被端到端验证。Tutorial 接入后应当删除本类与 ReviewDemoTargetHandler。
 *
 * 字段刻意保持业务无关：它证明 Review 只搬运视图、不解析业务字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDemoTargetView implements ReviewTargetView {

    private String viewType;

    private Long targetId;
    private String targetModule;
    private String targetType;
    private String revisionRef;

    private String title;
    private String summary;

    // 演示用版本内容，真实业务模块这里放的是章节列表、正文等
    private String contentSnapshot;

    private LocalDateTime frozenAt;

    @Override
    public String viewType() {
        return viewType;
    }
}
