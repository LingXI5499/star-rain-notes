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
 * 仅供 Review 单元测试断言冻结视图；生产环境使用 Tutorial 的视图。
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
