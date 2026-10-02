package com.starrainnotes.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 演示用的提交审核入参。
 *
 * Tutorial / Blog 模块尚未实现，REV-001 没有真实的浏览器侧调用方。
 * 为了让「提交审核」这条链路可以被端到端验证（含重复提交被拒），
 * Review 暂时提供一个受权限保护的演示入口，由 ReviewDemoTargetHandler 接单。
 *
 * 这个 DTO 明确不是正式契约：正式入口是业务模块自己验证对象权限后调用的
 * ReviewSubmissionApi.submit()，届时本 DTO 与对应端点一并移除。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDemoSubmissionDTO {

    @NotBlank
    @Size(max = 100)
    private String reviewType;

    @NotBlank
    @Size(max = 50)
    private String targetModule;

    @NotBlank
    @Size(max = 50)
    private String targetType;

    @NotNull
    private Long targetId;

    @NotBlank
    @Size(max = 128)
    private String targetRevisionRef;

    @NotBlank
    @Size(max = 255)
    private String targetDisplayName;

    @Size(max = 1000)
    private String submissionNote;
}
