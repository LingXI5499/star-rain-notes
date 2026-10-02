package com.starrainnotes.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * REV-005 审核拒绝入参。
 *
 * reason 必填：拒绝原因会原样写进 decision_reason 与动作历史，
 * 是申请人修改后重新提交的唯一依据。Service 仍会再校验一次，
 * 因为 ReviewRejectCommand 也可能由内部调用方直接构造。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRejectCommand {

    @NotBlank
    @Size(max = 2000)
    private String reason;
}
