package com.starrainnotes.review.service;

import com.starrainnotes.review.api.dto.ReviewSubmissionResult;
import com.starrainnotes.review.dto.ReviewDemoSubmissionDTO;

/*
 * 演示用提交服务（脚手架，Tutorial 接入后删除）。
 *
 * 正式边界是「业务模块验证对象权限后调用 ReviewSubmissionApi」，
 * 浏览器不应该自己指定 targetModule / targetId。但 Tutorial 模块目前只有空 pom，
 * 如果完全按终态实现，REV-001 就无法被端到端验证，重复提交、拒绝、取消、
 * SPI 回调这几条链路也都没有真实触发点。
 *
 * 因此这里提供一个受限的过渡入口：
 *   - 只接受 ReviewDemoTargetHandler 登记的 targetModule + targetType + reviewType；
 *   - 申请人账户ID 由服务端从当前认证主体取值，不接受请求参数；
 *   - 权限仍由 Controller 的 @PreAuthorize 把关。
 * 它证明的是「提交侧的核心不变量」，不是「浏览器可以自由提交任意目标」。
 */
public interface ReviewDemoSubmissionService {

    ReviewSubmissionResult submit(ReviewDemoSubmissionDTO request, Long applicantAccountId);
}
