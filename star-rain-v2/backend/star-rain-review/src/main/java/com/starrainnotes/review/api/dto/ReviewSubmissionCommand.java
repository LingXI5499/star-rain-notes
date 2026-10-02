package com.starrainnotes.review.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 提交审核的入参。
 *
 * 这个对象由业务 Service 构造，不是让浏览器任意提交 targetModule/targetId：
 * 业务模块必须先验证 actor 与对象级权限，再确定不可变 revisionRef，最后才调用
 * ReviewSubmissionApi.submit()。
 *
 * applicantAccountId 由业务模块从当前认证主体取值，浏览器无法伪造。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSubmissionCommand {

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

    // 提交时不可变版本引用；允许为空时业务模块必须自己保证「审核的就是当前版本」
    @NotBlank
    @Size(max = 128)
    private String targetRevisionRef;

    @NotBlank
    @Size(max = 255)
    private String targetDisplayName;

    @NotNull
    private Long applicantAccountId;

    @Size(max = 100)
    private String applicantDisplayName;

    @Size(max = 1000)
    private String submissionNote;
}
